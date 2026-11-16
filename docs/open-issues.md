# FINNEST — everything still open, in one list

_Written 2026-11-16. This is a plain list of what is not finished or not right.
Nothing here is hidden behind jargon. Each item says what happens, why it matters,
and roughly how much work it is._

Everything **not** on this list is done and passing CI: the website, the admin
console, the Android app, cross-device sync, sign-in, realtime, and the database
policies.

---

## A. Things that will break or mislead a real user

These are the ones to fix before showing the app to a buyer.

### A1. Password-reset and confirmation emails point at your laptop
**What happens:** Supabase's Site URL is still `http://localhost:3000`. A buyer who
clicks "forgot password" gets an email whose link opens nothing on their phone.
**Why it matters:** they conclude sign-up is broken and leave.
**Effort:** 2 minutes, no code. Set the Site URL and Redirect URLs in Supabase →
Authentication → URL Configuration.
**Where it is explained:** `docs/vercel-deployment.md`, Step 4.3.

### A2. Bulk import fails on Vercel unless a fifth setting is added
**What happens:** the CSV/Excel importer in the admin console runs on the server
and needs `SUPABASE_SERVICE_ROLE_KEY`. Without it, importing throws
"Missing Supabase environment variable(s)".
**Why it matters:** the deployment guide only listed four variables, so this would
have looked like a broken feature.
**Effort:** 5 minutes, no code.
**Status:** the guide now documents it, with a warning that this key bypasses all
security rules and must never go in a `VITE_*` variable.

### A3. The admin panel still tells staff not to use it
**What happens:** the admin panel shows "Full security testing is not yet complete;
do not use this panel for production operations."
**Why it matters:** either the statement is true — in which case staff should not
rely on the panel — or it is out of date and is now undermining confidence in work
that is actually done.
**Effort:** small, but it needs a decision from you, not a code change. If you
accept the current security posture (owner-only reads, two-factor-gated admin
writes, all proven by the policy tests), the sentence should be softened to
describe the real limits. If you do not, the panel has real gaps to close first.

### A4. Nothing reminds a buyer to come back
**What happens:** reminders are in-app dated notes. The app schedules no phone
notifications. The app is honest about this on the Profile and Reminders screens.
**Why it matters:** a buyer who is mid-decision forgets, and FINNEST is not in
front of them at the moment they would have returned.
**Effort:** medium. Needs notification permission, a scheduler, and a decision
about when to notify.

### A5. The project catalogue is empty
**What happens:** the Projects screen and the Android catalogue show nothing,
because no project has been published.
**Why it matters:** an empty catalogue looks unfinished to a first-time visitor.
**Effort:** no code. Add real projects in admin → Projects. **Do not** invent
sample listings: buyers would read them as real offers.

---

## B. Advertised features that do not exist

The app is careful to say so on screen, but they are still missing.

### B1. Expert contact, booking and messaging
**What happens:** the Experts directory shows "No experts are configured" and the
request button is permanently disabled reading "Request a conversation ·
unavailable". There is no messaging, contact sharing or payment.
**Why it matters:** if the business plan involves connecting buyers to lawyers or
advisors, this is the core of it and it does not exist.
**Effort:** large. Needs a real expert model, consent for sharing contact details,
and probably a payment or referral agreement.

### B2. Document upload
**What happens:** the database has a table for uploaded document records
(`finnest_documents`) and storage policies exist, but no screen anywhere lets a
buyer upload a file.
**Why it matters:** buyers are told to "track the conversation without uploading
personal documents here". That is a deliberate choice, not a bug — but it means
the document feature is half-built.
**Effort:** medium, and it carries a real risk: storing Aadhaar/PAN copies makes
you responsible for protecting identity documents.

### B3. Comparison detail
**What happens:** the shortlist holds up to five project names only. The screen
says "details not yet added".
**Why it matters:** "compare projects" is a headline promise of the product, and
right now comparison is a list of names.
**Effort:** medium. The database already has a projects table to draw from.

---

## C. Small inconsistencies (low risk, quick to fix)

### C1. Two draft tables, one dead
`finnest_user_drafts` is a leftover from the first design. Nothing reads or writes
it. `finnest_draft_fields` replaced it. Deleting the old table would remove a
confusing duplicate, but it also destroys any rows still sitting in it — check
first whether it holds anything you care about.

### C2. `last_seen_at` is read but never written
The admin user list selects `last_seen_at`, but nothing ever sets it and the
column is not displayed. So there is no way to see which buyers are returning.
If you want "who is active", this is the hook to use — it needs one line on sign-in
plus a column in the table.

### C3. A released app version is not buildable
`android/app/build.gradle.kts` has no release signing configuration. The release
workflow exists, but publishing to the Play Store needs a keystore, which is
deliberately not in the repository. This is correct practice, not a defect —
but it means the first real release needs that setup done once, by you.

---

## D. Things I could not verify from here

Not failures. Simply things that need a human or a live environment.

### D1. Nobody has signed in on two devices
The database guarantees are proven: a second user cannot read or write another
user's answers, and a stale offline write is rejected. Both apps are unit-tested.
But no person has yet typed an answer on a phone and watched it appear on a laptop.
**Do this before promising it to a customer.** Concretely: sign in on the website,
type a down payment, sign in on Android with the same account, open the same
journey, and confirm the figure is there. Then change it on the phone and reload
the website.

### D2. Realtime is configured, not watched
The tables are in the realtime publication and both clients subscribe. CI cannot
prove a message actually arrives at a browser. Same manual test as D1, without
reloading the page.

### D3. Two-factor isolation was tested with one simulated user set
The policy tests simulate roles and session levels inside PostgreSQL. They are
thorough, and I deliberately broke each guarantee to confirm the tests catch it.
But only two real accounts exist in the project. A test with two genuinely
separate people is stronger.

### D4. The Android emulator suite is flaky by nature
`ResaleJourneyTest` failed twice on a commit that only changed a Markdown file,
then passed after I made a screen transition explicit. Emulator tests on shared CI
runners are prone to this. Treat a single red run as "look again", not "the app is
broken".

---

## E. Before this goes public at all

Ordered, so working down the list makes sense.

1. Set the Site URL (A1). Two minutes, unblocks sign-up.
2. Add the fifth environment variable if you want bulk import (A2).
3. Publish real projects, or accept an empty catalogue (A5).
4. Do the two-device test (D1). This is the one that decides whether sync is real.
5. Decide what the admin panel should claim about its own security (A3).
6. Supabase's free tier **pauses after about a week of inactivity**. Upgrade before
   sharing a link, or the whole app will look empty and broken.
7. Write a privacy policy and a Play Store data-safety declaration. You are
   handling buyers' property plans and contact details.
8. Decide whether B1–B3 are in scope for launch. If not, the screens honestly say
   they are unavailable, which is acceptable — but it is a product decision, not a
   technical one.

---

## Summary in one line

The engineering is finished and proven: builds, tests, policies and the live
database all check out. What remains is **configuration** (A1, A2), **content**
(A5), **a manual test only a human can do** (D1), and **product decisions about
features that were never built** (B1–B3, A4).
