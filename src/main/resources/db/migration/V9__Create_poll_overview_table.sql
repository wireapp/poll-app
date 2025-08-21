create table poll_overview
(
    id              varchar(36) null,
    poll_id         varchar(36) unique not null references polls (id),
    results_visible boolean     not null,
    primary key (poll_id)
);

alter table polls drop column participation_message_id;
