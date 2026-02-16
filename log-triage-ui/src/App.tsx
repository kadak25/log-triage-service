import { useMemo, useState } from "react";
import api from "./api";
import type { AnalyzeResponse } from "./types";

const MAX_LOG_CHARS = 20000;

function badgeClass(sev?: string) {
  if (sev === "HIGH") return "badge badge-high";
  if (sev === "MED") return "badge badge-med";
  return "badge badge-low";
}

export default function App() {
  const [logContent, setLogContent] = useState<string>("");
  const [file, setFile] = useState<File | null>(null);

  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<AnalyzeResponse | null>(null);
  const [error, setError] = useState<string>("");

  const canAnalyzeText = useMemo(() => logContent.trim().length > 0, [logContent]);
  const canAnalyzeFile = useMemo(() => !!file, [file]);

  async function analyzeText() {
    setError("");
    setResult(null);
    setLoading(true);
    try {
      const res = await api.post<AnalyzeResponse>("/api/logs/analyze", { logContent });
      setResult(res.data);
    } catch (e: any) {
      setError(extractErr(e));
    } finally {
      setLoading(false);
    }
  }

  async function analyzeFile() {
    if (!file) return;
    setError("");
    setResult(null);
    setLoading(true);
    try {
      const form = new FormData();
      form.append("file", file);

      const res = await api.post<AnalyzeResponse>("/api/logs/analyze/file", form, {
        headers: { "Content-Type": "multipart/form-data" },
      });
      setResult(res.data);
    } catch (e: any) {
      setError(extractErr(e));
    } finally {
      setLoading(false);
    }
  }

  async function copy(text: string) {
    await navigator.clipboard.writeText(text);
    alert("Copied ✅");
  }

  return (
    <div className="page">
      <header className="header">
        <div>
          <h1>AI Log Reader / Incident Triage Assistant</h1>
          <p className="sub">
            Paste logs or upload a file → get severity, root cause hypothesis, next steps, grep queries and a ticket summary.
          </p>
        </div>
        {result?.severity && <span className={badgeClass(result.severity)}>{result.severity}</span>}
      </header>

      <div className="grid">
        <section className="card">
          <h2>Paste Log</h2>
          <textarea
            value={logContent}
            onChange={(e) => {
              const v = e.target.value;
              setLogContent(v.length > MAX_LOG_CHARS ? v.slice(0, MAX_LOG_CHARS) : v);
            }}
            placeholder={`Paste log here (max ${MAX_LOG_CHARS} chars)...`}
          />
          <div className="row">
            <button className="btn primary" disabled={loading || !canAnalyzeText} onClick={analyzeText}>
              {loading ? "Analyzing..." : "Analyze (Text)"}
            </button>
            <button
              className="btn"
              disabled={loading || logContent.length === 0}
              onClick={() => setLogContent("")}
            >
              Clear
            </button>
          </div>
        </section>

        <section className="card">
          <h2>Upload Log File</h2>
          <input
            type="file"
            accept=".log,.txt"
            onChange={(e) => setFile(e.target.files?.[0] ?? null)}
          />
          <div className="hint">
            Accepted: <b>.log</b>, <b>.txt</b> (server size limit applies)
          </div>
          <div className="row">
            <button className="btn primary" disabled={loading || !canAnalyzeFile} onClick={analyzeFile}>
              {loading ? "Uploading..." : "Analyze (File)"}
            </button>
            <button className="btn" disabled={loading || !file} onClick={() => setFile(null)}>
              Remove file
            </button>
          </div>
        </section>
      </div>

      {error && (
        <div className="alert error">
          <b>Error:</b> {error}
        </div>
      )}

      {result && (
        <div className="results">
          <section className="card">
            <h2>Findings</h2>
            <div className="kv">
              <div>
                <div className="k">Possible root cause</div>
                <div className="v">{result.possibleRootCause}</div>
              </div>
              <div>
                <div className="k">Detected issues</div>
                <ul className="v">
                  {result.detectedIssues?.map((x, i) => <li key={i}>{x}</li>)}
                </ul>
              </div>
            </div>

            {result.detectedIds && result.detectedIds.length > 0 && (
              <>
                <div className="divider" />
                <div className="k">Detected IDs</div>
                <div className="chips">
                  {result.detectedIds.map((id, i) => (
                    <span className="chip" key={i}>{id}</span>
                  ))}
                </div>
              </>
            )}
          </section>

          <section className="card">
            <h2>Top Error Signatures</h2>
            <table className="table">
              <thead>
                <tr>
                  <th>Exception</th>
                  <th>Message example</th>
                  <th>Count</th>
                </tr>
              </thead>
              <tbody>
                {result.topErrorSignatures?.map((s, i) => (
                  <tr key={i}>
                    <td><code>{s.exceptionType}</code></td>
                    <td className="muted">{s.message}</td>
                    <td>{s.count}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </section>

          <section className="card">
            <h2>Next Steps</h2>
            <ol>
              {result.nextSteps?.map((s, i) => <li key={i}>{s}</li>)}
            </ol>
          </section>

          {result.suggestedGrepQueries && result.suggestedGrepQueries.length > 0 && (
            <section className="card">
              <h2>Suggested grep queries</h2>
              <pre className="code">
                {result.suggestedGrepQueries.join("\n")}
              </pre>
              <div className="row">
                <button className="btn" onClick={() => copy(result.suggestedGrepQueries!.join("\n"))}>
                  Copy grep block
                </button>
              </div>
            </section>
          )}

          {(result.ticketTitle || result.ticketBody) && (
            <section className="card">
              <h2>Ticket Summary</h2>
              {result.ticketTitle && (
                <>
                  <div className="k">Title</div>
                  <div className="v"><b>{result.ticketTitle}</b></div>
                </>
              )}
              {result.ticketBody && (
                <>
                  <div className="divider" />
                  <div className="k">Body</div>
                  <pre className="code">{result.ticketBody}</pre>
                  <div className="row">
                    <button className="btn primary" onClick={() => copy((result.ticketTitle ? result.ticketTitle + "\n\n" : "") + (result.ticketBody ?? ""))}>
                      Copy ticket
                    </button>
                  </div>
                </>
              )}
            </section>
          )}

          <section className="card">
            <h2>AI / Telemetry</h2>
            <div className="kv">
              <div>
                <div className="k">aiUsed</div>
                <div className="v">{String(result.aiUsed)}</div>
              </div>
              <div>
                <div className="k">aiProvider</div>
                <div className="v">{result.aiProvider || "-"}</div>
              </div>
              <div>
                <div className="k">aiLatencyMs</div>
                <div className="v">{result.aiLatencyMs ?? "-"}</div>
              </div>
              <div>
                <div className="k">aiError</div>
                <div className="v">{result.aiError ?? "-"}</div>
              </div>
            </div>
          </section>
        </div>
      )}

      <footer className="footer">
        <span className="muted">Local demo UI • built for Application / Production Support workflows</span>
      </footer>
    </div>
  );
}

function extractErr(e: any): string {
  // axios error shape
  const msg =
    e?.response?.data?.message ||
    e?.response?.data?.error ||
    e?.message ||
    "Unknown error";
  const status = e?.response?.status;
  return status ? `HTTP ${status} - ${msg}` : String(msg);
}
