🚨 AI Log Reader / Incident Triage Assistant

A production-oriented log analysis and incident triage tool designed for Application Support, Production Support, and DevOps workflows.

Paste logs or upload log files to get severity, root cause hypothesis, next steps, grep queries, and a ready-to-use ticket summary.

✨ Features
🔍 Log Analysis

Paste raw log text or upload .log / .txt files

Automatic detection of:

NullPointerException

SQL and database connectivity issues

Network and socket timeouts

Error grouping and signature counting

🧠 AI-Assisted Incident Triage

Hugging Face LLM integration

AI-generated incident summary, likely root cause, and next steps

Safe fallback to rule-based analysis if AI is unavailable

🧾 Ticket Generation

Auto-generated incident title

Jira / ServiceNow ready ticket body

Context checklist:

Timestamp range

Correlation / Request ID

Environment and deployment version

🛠 Ops-Friendly Output

Suggested grep commands

Copy-to-clipboard outputs

Severity badge (LOW / MEDIUM / HIGH)

AI telemetry (provider, latency, fallback)

🧱 Architecture
Backend

Java 17

Spring Boot

Rule-based analyzer (regex and heuristics)

Hugging Face AI client

File upload and validation

Size limits and error handling

Frontend

React

Dark UI inspired by real support dashboards

Textarea and file upload

Result cards for analysis output

🔌 API Endpoints
### Analyze pasted logs
POST /api/logs/analyze
Content-Type: application/json
```http
{
  "logContent": "..."
}

Analyze uploaded file
POST /api/logs/analyze/file
Content-Type: multipart/form-data

file=@app.log
```


Accepted file types: .log, .txt
Server-side size limits apply.

🧪 Example Detected Issues

NullPointerException

SQLTransientConnectionException

SocketTimeoutException

Connection pool exhaustion

Unhandled global exceptions

🧠 Why This Project?

This project mirrors real production support workflows:

Logs arrive → triage → summarize → open a ticket

It focuses on system thinking and real-world incident response rather than toy demos.

🚀 Roadmap

Rate limiting per endpoint

Log fixtures dataset (20+ real-world examples)

Docker and CI pipeline

Global exception response standardization

📸 Screenshots

See the /screenshots folder for UI and API examples.

<img width="1915" height="939" alt="db394b75-8ce9-4ad5-8844-d0f33d7bdc58" src="https://github.com/user-attachments/assets/eaa3bab1-1fd3-4695-857d-7e6815c46f88" />
<img width="1360" height="817" alt="9230e554-72c3-4c3c-b557-c1b90d3c5cbd" src="https://github.com/user-attachments/assets/13563be8-573f-4ec6-ae91-f5044b8d52e8" />
<img width="1259" height="484" alt="e59b03ab-9609-46c4-b71c-237a6ac53b6d" src="https://github.com/user-attachments/assets/8c6332ba-5a1e-454e-97ac-220abf85bb80" />
<img width="1354" height="821" alt="3fd41ab2-ee3f-4795-bb93-c32e9e4769e7" src="https://github.com/user-attachments/assets/fce63c87-bbd1-4c6c-85e3-005b89d29842" />

⚠️ Notes

Hugging Face token is read from environment variable HF_TOKEN

No secrets are committed to the repository

👤 Author

Mustafa Kadak
Backend / Application Support / DevOps-oriented Engineer
