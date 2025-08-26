alter table polls add column results_visible boolean;
alter table polls rename column participation_message_id to overview_message_id;
