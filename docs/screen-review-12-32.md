# Prototype review: screens 12–32

Reviewed the supplied Drive images visually. This is an overview and ambiguity log, not a complete extraction of every field, interaction or hidden menu. The screenshot is the primary reference; where it is ambiguous, do not silently substitute content from the supporting document.

## New Property

| Image | Visible screen | Notes / implementation concerns |
|---|---|---|
| 12 | Step 4B: Resident feedback notes | Topic dropdown (closed; options not visible), optional date and context, what was shared, concern/question, follow-up, add note, notes entered, and a disclaimer. The screenshot does not show expanded options, added-note editing/deletion behavior, or field validation. |
| 14 | Step 6: Legal due diligence | Checklist status, note/source/date for title, documents, encumbrances, plans, RERA, approvals, agreement, payment schedule and possession/cancellation/refund clauses; lawyer guidance. Status options are not expanded. |
| 13 | Step 5: Home loan guide | Loan amount, interest rate, tenure, optional lender and planning context, illustrative EMI, lender questions and document prompts. Formula and input limits require verification before implementation; the estimate is not loan approval. |
| 16 | Step 8: Final review and next actions | Aggregates statuses, budget/loan assumptions, saved projects and notes, pending items, concerns, professional review and user actions. This must reflect real data without implying independent verification. |
| 15 | Step 7: Financial due diligence | One-time and recurring estimates, cost/document prompts and loan comparison. Empty values remain unknown. Some controls have closed dropdowns. |

File order is not journey-step order: images 13–16 show steps 5, 6, 7 and 8 respectively.

## Resale and shared screens

| Image | Visible screen | Notes / implementation concerns |
|---|---|---|
| 17 | Resale journey overview | Six confirmed subject areas; explicitly says the prototype does not define a complete resale journey. This conflicts with the supporting document's separate 12-step tracker; confirm how they map. |
| 18 | Resale requirements and location | Optional budget/cost estimates, configuration/practical needs and location priorities. |
| 19 | Condition inspection | Checklist observations with status, notes, source and date; not a technical inspection. |
| 20 | Seller, ownership and title | Seller authority, ownership records, loan/encumbrance, co-owner consent and property identifiers. |
| 21 | Society documents and dues | Membership records, maintenance/property dues, transfer, parking, repairs and disputes; unknowns must not become zero. |
| 22 | Legal review | Lawyer discussion prompts and review status; dropdown options are closed. |
| 23 | Agreement and handover | Agreement, promises, payment, possession, registration, loan release, keys, utilities and handover prompts; dropdown options are closed. |
| 24 | Experts directory | Informational categories and filters; no configured expert profiles or contact details in the prototype. |
| 25 | Expert detail | No configured expert; contact request disabled, no booking/payment service connected in the prototype. |
| 26 | Journey summary | Journey type, user-entered progress, filter, journey screens and projects. |
| 27 | Reminders | User-entered reminder/date/optional note; prototype does not schedule OS notifications. |
| 28 | Privacy and consent | Device-save choice and separate optional contact, marketing and analytics choices; prototype-only statements must not be copied as claims about the actual backend. |
| 29 | About and disclaimer | Decision-support limits and prototype data notice. |
| 30 | Profile and settings | Profile, device save, notification preferences, links and reset/delete journey data. |
| 31 | Risk detail | Illustrative verbal-promise concern, questions and related checklist item; no finding or verification claim. |
| 32 | Helpful states | Empty, loading, error and success copy examples; success must only follow confirmed persistence. |

## Blocker for the next Android screen

Screen 12's Topic selector is closed. Neither its choices nor the exact required-field/added-note rules are specified by this image. The supporting content lists example questions for residents, but does not establish the selector's exact choices. The master brief says to ask when a screenshot is unclear and not invent missing options. The next step therefore cannot be faithfully completed and tested without the expanded menu or an explicit decision to use a different input. No changes were made to live databases or guest data as part of this review.

## Decision for Step 4B

The user authorized discretion on the missing menu. Android uses a free-text topic instead of invented dropdown options. Topic and what was shared are required to add a note; the other fields are optional. These are explicit implementation choices, not claims about hidden prototype behavior. The current form autosaves unfinished input locally, permits deletion of added notes after confirmation, and leads to the loan guide. Cloud sync and device-level validation remain outstanding.

## Step 5 implementation decisions

Android uses a fixed-rate monthly amortization formula including a zero-interest case. Amount must be positive and at most ₹1 trillion, annual rate 0–100%, and tenure a whole number from 1–50 years; these are safety limits, not lender criteria. Income and obligations are optional context and do not affect the calculation. The closed lender-document status menu uses the same provisional labels already used in earlier screens, not verified prototype options. Inputs stay on device; no lender offer, eligibility check, fee estimate or document upload is provided.

## Step 6 implementation decisions

Android uses nine screenshot prompts and the same provisional status labels as earlier checklists, with locally saved optional note, source and follow-up date. These are user records, not legal findings. No lawyer profiles or referral service are configured, so the screen does not claim to connect users. Step 7 follows this checklist.

## Step 7 implementation decisions

Android stores separate one-time and monthly amounts, ten cost/document prompts and one set of lender terms locally. Totals include only entered values and display the entered count; no entries means not available, while malformed values prevent a total. Amounts accept up to two decimal places and ₹1 trillion per field; these are implementation limits, not financial advice. Status options are provisional. The screenshot shows one lender-term record, not a multi-offer comparison; no lender offers are supplied. Final review follows this step.

## Step 8 implementation decisions

The Android review derives checklist status counts and pending entries from existing local draft keys, shows buyer-entered notes, project names, loan assumptions and partial initial cost total, and autosaves next actions. It does not conflate provisional “Professional review” with verified evidence or claim project comparisons exist when only names are recorded. Concern detection, risk detail and expert directory/referrals are not yet implemented; the screen says so rather than presenting nonfunctional controls. Unreadable lists are reported without overwriting their stored data.

## Journey summary implementation

Android My Journey shows New Property local checklist status counts, a filter, project names and links to the current journey screens. Pending includes unselected statuses; other saved statuses remain distinct. Counts describe user-entered statuses rather than completed steps or verified evidence. Resale, cross-device sync and shared tab navigation are not implemented.
