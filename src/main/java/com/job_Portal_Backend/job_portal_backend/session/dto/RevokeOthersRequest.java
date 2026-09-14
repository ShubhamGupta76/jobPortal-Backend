package com.job_Portal_Backend.job_portal_backend.session.dto;

import lombok.Data;

/**
 * currentSessionId is optional: when the caller doesn't have it (e.g. an older client build),
 * revoke-others degrades to revoking every session, since there is no other way to identify
 * "this" session from a stateless access token alone.
 */
@Data
public class RevokeOthersRequest {
    private Long currentSessionId;
}
