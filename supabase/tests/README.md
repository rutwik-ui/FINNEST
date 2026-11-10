# Disposable database policy tests

The GitHub Actions `Database policy tests` workflow creates an isolated PostgreSQL 15 service, installs a **minimal test shim** for `auth` and `storage`, applies this repository's real migrations, and runs three transactional SQL suites. The database and fake identities are discarded after the job. Never run the shim or test fixtures on a real Supabase project.

Coverage includes:

- Private draft owner read/write/delete, cross-user denial, missing identity, and basic constraints.
- Object-row policies for owner folders, cross-user read/write/delete and move denial, root/prefix denial, and bucket settings.
- Published vs draft guidance visibility, anon/buyer/support denial, editor AAL1 denial, editor AAL2 insert/update, audit trigger behavior, unpublishing, role revocation and basic input constraints.

This is behavioral PostgreSQL RLS testing against **simulated** JWT claims and a minimal storage schema, not a real Supabase deployment. It does not verify Auth token issuance, MFA enrollment, PostgREST, Storage APIs, file contents/MIME/size enforcement during actual uploads, session behavior, backups, production migration drift, or two-user isolation in the connected project. Use a separate authorized staging project and real test identities for those checks before production. AAL values here are supplied directly as test claims; this does not prove they can be obtained securely.
