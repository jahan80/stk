-- ==================================================
-- V3: Group memberships
-- ==================================================

CREATE TABLE ticket.ticket_group_members (
    id              BIGSERIAL    PRIMARY KEY,
    group_id        BIGINT       NOT NULL,
    user_id         BIGINT       NOT NULL,
    role            VARCHAR(20)  NOT NULL DEFAULT 'AGENT',   -- LEADER, AGENT
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_gm_group
        FOREIGN KEY (group_id) REFERENCES ticket.ticket_groups(id) ON DELETE CASCADE,

    CONSTRAINT uk_gm_group_user UNIQUE (group_id, user_id)
);

CREATE INDEX idx_gm_group ON ticket.ticket_group_members(group_id);
CREATE INDEX idx_gm_user  ON ticket.ticket_group_members(user_id);

COMMENT ON TABLE ticket.ticket_group_members IS 'Group members';
COMMENT ON COLUMN ticket.ticket_group_members.role IS 'LEADER or AGENT';
