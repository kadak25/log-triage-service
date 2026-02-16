# 🚨 AI Log Reader / Incident Triage Assistant
A production-oriented log analysis and incident triage tool designed for **Application Support, Production Support, and DevOps** workflows. 
Paste logs or upload log files → get **severity**, **root cause hypothesis**, **next steps**, **grep queries**, and a **ready-to-use ticket summary**. 
--- ## ✨ Features ### 
🔍 Log Analysis - Paste raw log text **or** upload .log / .txt files - Automatic detection of: - Exceptions (NPE, SQL, Timeout, etc.) - Database connectivity issues - Network / socket timeouts - Error **grouping & signature counting** ### 
🧠 AI-Assisted Incident Triage - Hugging Face LLM integration (via router API) - AI-generated: - Incident summary - Likely root cause - Actionable next steps - **Safe fallback** to rule-based analysis if AI is unavailable ### 
🧾 Ticket Generation - Auto-generated **incident title** - Clean **ticket body** (Jira / ServiceNow ready) - Context checklist: - Timestamp range - Correlation / Request ID - Environment & deployment version ### 
🛠️ Ops-Friendly Output - Suggested grep commands - Copy-to-clipboard buttons - Clear severity badge (LOW / MEDIUM / HIGH) - AI telemetry (latency, provider, fallback status) --- 
## 🧱 Architecture **Backend** - Java 17 - Spring Boot - Rule-based analyzer (regex & heuristics) - Hugging Face AI client (chat completions) - File upload support - Size limits & validation **Frontend** - React - Clean dark UI inspired by real support dashboards - Textarea + file upload - Result cards for findings, signatures, steps, and ticket summary --- 
## 🔌 API Endpoints ### Analyze pasted logs
http
POST /api/logs/analyze
Content-Type: application/json

{
  "logContent": "..."
}
Analyze uploaded file
POST /api/logs/analyze/file
Content-Type: multipart/form-data
🧪 Example Detected Issues
NullPointerException

SQLTransientConnectionException

SocketTimeoutException

Connection pool exhaustion

Unhandled global exceptions

🧠 Why This Project?
This project mirrors real production support workflows:

Logs arrive → you triage → you summarize → you open a ticket.

Shows system thinking, not just CRUD or toy demos.

Demonstrates AI + fallback design suitable for production environments.

🚀 Roadmap
Rate limiting (per endpoint)

Log fixtures dataset (20+ real-world examples)

Docker & CI pipeline

Global exception response standardization

📸 Screenshots
See /screenshots folder for UI and API examples.
<img width="1915" height="939" alt="db394b75-8ce9-4ad5-8844-d0f33d7bdc58" src="https://github.com/user-attachments/assets/5e47ffd6-1a75-4c40-bae7-69923e63efa8" />
<img width="1360" height="817" alt="9230e554-72c3-4c3c-b557-c1b90d3c5cbd" src="https://github.com/user-attachments/assets/ba131b3c-800d-4abd-af56-eb7d5903254e" />
<img width="1259" height="484" alt="e59b03ab-9609-46c4-b71c-237a6ac53b6d" src="https://github.com/user-attachments/assets/992ef698-4385-42c8-807d-3585681d423b" />
<img width="1354" height="821" alt="3fd41ab2-ee3f-4795-bb93-c32e9e4769e7" src="https://github.com/user-attachments/assets/c36d5855-2344-4db6-a011-bf509d6c0948" />


⚠️ Notes
Hugging Face token is read from environment variable: HF_TOKEN

No secrets are committed to the repository

👤 Author
Mustafa Kadak
Backend / Application Support / DevOps-oriented Engineer
