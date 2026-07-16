-- ============================================================
-- convoX — Supabase database setup
-- Paste this ENTIRE file into: Supabase dashboard -> SQL Editor
-- -> New query -> Run. One time only.
-- ============================================================

-- ---------- TABLES ----------

create table public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  username text not null,
  username_lower text not null unique,
  created_at timestamptz default now()
);

create table public.friend_requests (
  id text primary key,                -- "{from_uid}_{to_uid}"
  from_uid uuid not null references public.profiles(id),
  from_username text not null,
  to_uid uuid not null references public.profiles(id),
  to_username text not null,
  status text not null default 'pending'
    check (status in ('pending','accepted','rejected')),
  created_at timestamptz default now()
);

create table public.chats (
  id text primary key,                -- sorted "{uidA}_{uidB}"
  user_a uuid not null references public.profiles(id),
  user_b uuid not null references public.profiles(id),
  username_a text not null,
  username_b text not null,
  last_message text not null default '',
  last_message_time bigint not null default 0
);

create table public.messages (
  id bigint generated always as identity primary key,
  chat_id text not null references public.chats(id) on delete cascade,
  sender_id uuid not null references public.profiles(id),
  text text not null,
  timestamp bigint not null
);

create index messages_chat_idx on public.messages(chat_id, timestamp);

-- ---------- ROW LEVEL SECURITY ----------

alter table public.profiles enable row level security;
alter table public.friend_requests enable row level security;
alter table public.chats enable row level security;
alter table public.messages enable row level security;

-- profiles: any signed-in user can read (needed for username search);
-- inserts happen only through the claim_username function below.
create policy "profiles readable by signed-in users"
  on public.profiles for select to authenticated using (true);

-- friend_requests: only sender/recipient can see; sender creates;
-- recipient (or sender) can update status.
create policy "requests visible to parties"
  on public.friend_requests for select to authenticated
  using (auth.uid() = from_uid or auth.uid() = to_uid);

create policy "requests created by sender"
  on public.friend_requests for insert to authenticated
  with check (auth.uid() = from_uid and status = 'pending');

create policy "requests updatable by parties"
  on public.friend_requests for update to authenticated
  using (auth.uid() = from_uid or auth.uid() = to_uid);

-- chats: only participants can see/update; creation happens through
-- the accept_friend_request function below.
create policy "chats visible to participants"
  on public.chats for select to authenticated
  using (auth.uid() = user_a or auth.uid() = user_b);

create policy "chats updatable by participants"
  on public.chats for update to authenticated
  using (auth.uid() = user_a or auth.uid() = user_b);

-- messages: only chat participants can read; only participants can
-- send, and only as themselves.
create policy "messages visible to chat participants"
  on public.messages for select to authenticated
  using (exists (
    select 1 from public.chats c
    where c.id = chat_id and (c.user_a = auth.uid() or c.user_b = auth.uid())
  ));

create policy "messages sent by participants as themselves"
  on public.messages for insert to authenticated
  with check (
    sender_id = auth.uid()
    and exists (
      select 1 from public.chats c
      where c.id = chat_id and (c.user_a = auth.uid() or c.user_b = auth.uid())
    )
  );

-- ---------- FUNCTIONS ----------

-- Login by username: resolves a username to the account email.
-- SECURITY DEFINER so it can read auth.users without exposing the table.
create or replace function public.get_login_email(uname text)
returns text
language sql
security definer
set search_path = public
as $$
  select u.email::text
  from auth.users u
  join public.profiles p on p.id = u.id
  where p.username_lower = lower(uname)
  limit 1;
$$;

-- Claim a unique username right after signup. The UNIQUE constraint
-- on username_lower guarantees no duplicates even in a race.
create or replace function public.claim_username(uname text)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  if auth.uid() is null then
    raise exception 'Not signed in';
  end if;
  insert into public.profiles (id, username, username_lower)
  values (auth.uid(), uname, lower(uname));
end;
$$;

-- Accept a friend request: flips status and creates the shared chat.
-- Runs atomically server-side; only the recipient can accept.
create or replace function public.accept_friend_request(req_id text)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
  r record;
  cid text;
begin
  select * into r from public.friend_requests where id = req_id;
  if r is null then
    raise exception 'Request not found';
  end if;
  if r.to_uid <> auth.uid() then
    raise exception 'Only the recipient can accept';
  end if;
  if r.status <> 'pending' then
    raise exception 'Request is not pending';
  end if;

  update public.friend_requests set status = 'accepted' where id = req_id;

  if r.from_uid::text < r.to_uid::text then
    cid := r.from_uid::text || '_' || r.to_uid::text;
  else
    cid := r.to_uid::text || '_' || r.from_uid::text;
  end if;

  insert into public.chats (id, user_a, user_b, username_a, username_b, last_message_time)
  values (cid, r.from_uid, r.to_uid, r.from_username, r.to_username,
          (extract(epoch from now()) * 1000)::bigint)
  on conflict (id) do nothing;
end;
$$;

-- ---------- REALTIME ----------
-- Enable live updates for the tables the app listens to.

alter publication supabase_realtime add table public.messages;
alter publication supabase_realtime add table public.chats;
alter publication supabase_realtime add table public.friend_requests;

-- ---------- TYPING INDICATOR ----------
create table public.typing (
  chat_id text not null references public.chats(id) on delete cascade,
  user_id uuid not null references public.profiles(id),
  updated_at bigint not null default 0,
  primary key (chat_id, user_id)
);

alter table public.typing enable row level security;

create policy "typing visible to chat participants"
  on public.typing for select to authenticated
  using (exists (
    select 1 from public.chats c
    where c.id = chat_id and (c.user_a = auth.uid() or c.user_b = auth.uid())
  ));

create policy "typing writable as self"
  on public.typing for insert to authenticated
  with check (user_id = auth.uid());

create policy "typing updatable as self"
  on public.typing for update to authenticated
  using (user_id = auth.uid());

-- ---------- PRESENCE + SEEN TICKS ----------
alter table public.profiles add column last_seen bigint not null default 0;

create policy "profiles updatable by self"
  on public.profiles for update to authenticated
  using (id = auth.uid());

alter table public.messages add column seen boolean not null default false;

create policy "messages seen-flag updatable by recipient"
  on public.messages for update to authenticated
  using (
    sender_id <> auth.uid()
    and exists (
      select 1 from public.chats c
      where c.id = chat_id and (c.user_a = auth.uid() or c.user_b = auth.uid())
    )
  );

-- ---------- PROFILE ABOUT ----------
alter table public.profiles add column about text not null default 'Hey there! I am using convoX.';
