# Attendance & Monthly Deposit Backend APIs

All endpoints use the existing JWT Bearer token. Admin management APIs require `ROLE_ADMIN`; self-service APIs return data for the logged-in user only.

## Attendance (Admin)
- `POST /api/attendance/bulk`
  ```json
  {"meetingId": 1, "entries": [{"userId": 5, "status": "PRESENT"}, {"userId": 6, "status": "ABSENT"}]}
  ```
  Supported statuses: `PRESENT`, `ABSENT`, `EXCUSED`. Re-submitting the same meeting/member updates the existing record.
- `GET /api/attendance/meeting/{meetingId}` — attendance for a meeting.
- `GET /api/attendance/dashboard?month=2026-10` — organization-wide attendance summary.

## Monthly deposits (Admin)
- `PUT /api/deposits/settings`
  ```json
  {"monthlyAmount": 1000.00, "effectiveFrom": "2026-10-01"}
  ```
  Settings are effective from the supplied date; month calculations use the setting effective on the first day of that month.
- `GET /api/deposits/settings` — settings history.
- `GET /api/deposits?month=2026-10` — member-wise monthly expected/paid/pending balances. Configure a setting before using this endpoint.
- `GET /api/deposits/dashboard?month=2026-10` — organization summary plus member rows.
- `POST /api/deposits/payment`
  ```json
  {"userId": 5, "month": "2026-10-01", "amount": 500.00, "paymentDate": "2026-10-24"}
  ```
  Amount is added to the existing monthly total; total payments cannot exceed the expected monthly amount.

## Member self-service
- `GET /api/attendance/me` — the logged-in member's attendance history.
- `GET /api/deposits/me` — the logged-in member's deposit history.
- `GET /api/dashboard/me?month=2026-10` — combined current-month summary and histories.

`month` query values use `YYYY-MM`. The deposit request's `month` should be the first day of the target month (e.g. `2026-10-01`). Existing `meetings` and `meeting_attendance` tables are reused. Migration `V4__attendance_and_monthly_deposits.sql` adds the new deposit tables and `marked_by` audit column.