package com.teamcanvas.workspace;

/** Role of a user inside one workspace. OWNER can never be assigned through the API (no ownership transfer in the MVP). */
public enum WorkspaceRole {
    OWNER,
    ADMIN,
    MEMBER
}
