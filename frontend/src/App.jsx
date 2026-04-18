import { useState, useEffect } from "react";

const API = "/api";

const STATUS_COLORS = {
    STARTED: { bg: "#FEF3C7", text: "#92400E", label: "Started" },
    IN_REVIEW: { bg: "#DBEAFE", text: "#1E40AF", label: "In Review" },
    APPROVED: { bg: "#D1FAE5", text: "#065F46", label: "Approved" },
    REJECTED: { bg: "#FEE2E2", text: "#991B1B", label: "Rejected" },
};

const REJECTION_REASONS = [
    "CUSTOMER_TOO_OLD",
];

function Badge({ status }) {
    const s = STATUS_COLORS[status] || { bg: "#F3F4F6", text: "#374151", label: status };
    return (
        <span style={{ background: s.bg, color: s.text, padding: "2px 10px", borderRadius: 999, fontSize: 12, fontWeight: 600 }}>
      {s.label}
    </span>
    );
}

function Btn({ onClick, children, variant = "primary", small, disabled }) {
    const styles = {
        primary: { background: "#2563EB", color: "#fff" },
        ghost: { background: "#F3F4F6", color: "#374151" },
        danger: { background: "#EF4444", color: "#fff" },
        success: { background: "#10B981", color: "#fff" },
    };
    return (
        <button onClick={onClick} disabled={disabled} style={{
            ...styles[variant],
            border: "none", borderRadius: 8,
            padding: small ? "5px 10px" : "10px 16px",
            fontSize: small ? 12 : 14,
            cursor: disabled ? "not-allowed" : "pointer",
            fontWeight: 600, opacity: disabled ? 0.6 : 1,
        }}>
            {children}
        </button>
    );
}

function formatMoney(n) {
    return new Intl.NumberFormat("en-EE", { style: "currency", currency: "EUR" }).format(n || 0);
}

function NewLoanModal({ onClose, onCreated }) {
    const [form, setForm] = useState({
        firstName: "", lastName: "", personalCode: "",
        loanAmount: "", loanPeriodMonths: "", interestMargin: "", baseInterestRate: "",
    });
    const [errors, setErrors] = useState({});
    const [globalError, setGlobalError] = useState(null);
    const [loading, setLoading] = useState(false);

    function set(field, value) {
        setForm((f) => ({ ...f, [field]: value }));
        setErrors((e) => ({ ...e, [field]: null }));
    }

    async function submit() {
        setLoading(true); setErrors({}); setGlobalError(null);
        try {
            const res = await fetch(`${API}/loans`, {
                method: "POST", headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    ...form,
                    loanAmount: parseFloat(form.loanAmount),
                    loanPeriodMonths: parseInt(form.loanPeriodMonths),
                    interestMargin: parseFloat(form.interestMargin),
                    baseInterestRate: parseFloat(form.baseInterestRate),
                }),
            });
            if (!res.ok) {
                const err = await res.json();
                if (err.errors) setErrors(err.errors);
                else setGlobalError(err.message || err.errorCode || "Error");
            } else { onCreated(); onClose(); }
        } catch { setGlobalError("Network error"); }
        setLoading(false);
    }

    const fields = [
        { label: "First Name", field: "firstName", type: "text" },
        { label: "Last Name", field: "lastName", type: "text" },
        { label: "Personal Code", field: "personalCode", type: "text", placeholder: "37605030299" },
        { label: "Loan Amount (min 5000 €)", field: "loanAmount", type: "number", placeholder: "5000" },
        { label: "Period in months (6–360)", field: "loanPeriodMonths", type: "number", placeholder: "12" },
        { label: "Interest Margin (%)", field: "interestMargin", type: "number", placeholder: "1.001" }
    ];

    return (
        <div style={{ position: "fixed", inset: 0, background: "rgba(0,0,0,0.4)", display: "flex", alignItems: "center", justifyContent: "center", zIndex: 1000 }} onClick={onClose}>
            <div style={{ background: "#fff", borderRadius: 12, padding: 32, width: 480, display: "flex", flexDirection: "column", gap: 14, maxHeight: "90vh", overflowY: "auto" }} onClick={(e) => e.stopPropagation()}>
                <h2 style={{ margin: 0, fontSize: 18 }}>New Loan Application</h2>
                {globalError && <div style={{ background: "#FEE2E2", color: "#991B1B", padding: "8px 12px", borderRadius: 8, fontSize: 13 }}>{globalError}</div>}
                {fields.map(({ label, field, type, placeholder }) => (
                    <div key={field} style={{ display: "flex", flexDirection: "column", gap: 4 }}>
                        <label style={{ fontSize: 13, color: "#6B7280" }}>{label}</label>
                        <input type={type} placeholder={placeholder} value={form[field]} onChange={(e) => set(field, e.target.value)} style={{ padding: "8px 12px", borderRadius: 8, border: `1px solid ${errors[field] ? "#F87171" : "#E5E7EB"}`, fontSize: 14, outline: "none", background: errors[field] ? "#FFF5F5" : "#fff" }} />
                        {errors[field] && <span style={{ fontSize: 12, color: "#EF4444" }}>{errors[field]}</span>}
                    </div>
                ))}
                <div style={{ display: "flex", gap: 8, justifyContent: "flex-end", marginTop: 8 }}>
                    <Btn variant="ghost" onClick={onClose}>Cancel</Btn>
                    <Btn onClick={submit} disabled={loading}>{loading ? "Submitting..." : "Submit"}</Btn>
                </div>
            </div>
        </div>
    );
}

function LoanDetailModal({ loan, onClose, onUpdated }) {
    const [rejectReason, setRejectReason] = useState("CUSTOMER_TOO_OLD");
    const [showReject, setShowReject] = useState(false);
    const [regenForm, setRegenForm] = useState({ loanPeriodMonths: loan.loanPeriodMonths, interestMargin: loan.interestMargin, baseInterestRate: loan.baseInterestRate });
    const [schedule, setSchedule] = useState(loan.paymentSchedule || []);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);

    const canReview = loan.status === "IN_REVIEW";

    async function approve() {
        setLoading(true); setError(null);
        try {
            const res = await fetch(`${API}/loans/${loan.id}/review/approve`, { method: "POST" });
            if (!res.ok) { const e = await res.json(); setError(e.message || e.errorCode || "Error"); }
            else { onUpdated(); onClose(); }
        } catch { setError("Network error"); }
        setLoading(false);
    }

    async function reject() {
        setLoading(true); setError(null);
        try {
            const res = await fetch(`${API}/loans/${loan.id}/review/reject`, {
                method: "POST", headers: { "Content-Type": "application/json" },
                body: JSON.stringify(rejectReason),
            });
            if (!res.ok) { const e = await res.json(); setError(e.message || e.errorCode || "Error"); }
            else { onUpdated(); onClose(); }
        } catch { setError("Network error"); }
        setLoading(false);
    }

    async function regenerate() {
        setLoading(true); setError(null);
        try {
            const res = await fetch(`${API}/loans/${loan.id}/schedule/regenerate`, {
                method: "POST", headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    loanPeriodMonths: parseInt(regenForm.loanPeriodMonths),
                    interestMargin: parseFloat(regenForm.interestMargin),
                    baseInterestRate: parseFloat(regenForm.baseInterestRate),
                }),
            });
            if (!res.ok) { const e = await res.json(); setError(e.message || e.errorCode || "Error"); }
            else { const data = await res.json(); setSchedule(data); onUpdated(); }
        } catch { setError("Network error"); }
        setLoading(false);
    }

    return (
        <div style={{ position: "fixed", inset: 0, background: "rgba(0,0,0,0.4)", display: "flex", alignItems: "center", justifyContent: "center", zIndex: 1000 }} onClick={onClose}>
            <div style={{ background: "#fff", borderRadius: 12, padding: 32, width: 700, maxHeight: "90vh", overflowY: "auto", display: "flex", flexDirection: "column", gap: 20 }} onClick={(e) => e.stopPropagation()}>

                {/* Header */}
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                    <h2 style={{ margin: 0, fontSize: 18 }}>Loan #{loan.id} — {loan.firstName} {loan.lastName}</h2>
                    <Badge status={loan.status} />
                </div>

                {error && <div style={{ background: "#FEE2E2", color: "#991B1B", padding: "8px 12px", borderRadius: 8, fontSize: 13 }}>{error}</div>}

                {/* Info */}
                <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12 }}>
                    {[
                        ["Personal Code", loan.personalCode],
                        ["Amount", formatMoney(loan.loanAmount)],
                        ["Period", `${loan.loanPeriodMonths} months`],
                        ["Interest Margin", `${loan.interestMargin}%`],
                        ["Created", new Date(loan.createdAt).toLocaleDateString()],
                    ].map(([k, v]) => (
                        <div key={k} style={{ background: "#F9FAFB", borderRadius: 8, padding: "10px 14px" }}>
                            <div style={{ fontSize: 11, color: "#9CA3AF", marginBottom: 2 }}>{k}</div>
                            <div style={{ fontSize: 14, fontWeight: 600 }}>{v}</div>
                        </div>
                    ))}
                </div>

                {/* Regenerate schedule */}
                {canReview && (
                    <div style={{ border: "1px solid #E5E7EB", borderRadius: 10, padding: 16 }}>
                        <div style={{ fontSize: 14, fontWeight: 600, marginBottom: 12 }}>Regenerate Schedule</div>
                        <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr 1fr", gap: 10, marginBottom: 12 }}>
                            {[
                                { label: "Period (months)", field: "loanPeriodMonths" },
                                { label: "Interest Margin (%)", field: "interestMargin" }
                            ].map(({ label, field }) => (
                                <div key={field} style={{ display: "flex", flexDirection: "column", gap: 4 }}>
                                    <label style={{ fontSize: 12, color: "#6B7280" }}>{label}</label>
                                    <input type="number" value={regenForm[field]} onChange={(e) => setRegenForm((f) => ({ ...f, [field]: e.target.value }))}
                                           style={{ padding: "6px 10px", borderRadius: 8, border: "1px solid #E5E7EB", fontSize: 13, outline: "none" }} />
                                </div>
                            ))}
                        </div>
                        <Btn small onClick={regenerate} disabled={loading}>Regenerate</Btn>
                    </div>
                )}

                {/* Payment schedule */}
                {schedule.length > 0 && (
                    <div>
                        <div style={{ fontSize: 14, fontWeight: 600, marginBottom: 10 }}>Payment Schedule</div>
                        <div style={{ overflowX: "auto" }}>
                            <table style={{ width: "100%", borderCollapse: "collapse", fontSize: 13 }}>
                                <thead>
                                <tr style={{ background: "#F3F4F6" }}>
                                    {["#", "Date", "Principal", "Interest", "Total", "Remaining"].map((h) => (
                                        <th key={h} style={{ padding: "8px 10px", textAlign: "left", color: "#6B7280", fontWeight: 600 }}>{h}</th>
                                    ))}
                                </tr>
                                </thead>
                                <tbody>
                                {schedule.map((row, i) => (
                                    <tr key={row.id || i} style={{ borderTop: "1px solid #F3F4F6" }}>
                                        <td style={{ padding: "7px 10px" }}>{row.paymentNumber}</td>
                                        <td style={{ padding: "7px 10px" }}>{row.paymentDate}</td>
                                        <td style={{ padding: "7px 10px" }}>{formatMoney(row.principalAmount)}</td>
                                        <td style={{ padding: "7px 10px" }}>{formatMoney(row.interestAmount)}</td>
                                        <td style={{ padding: "7px 10px" }}>{formatMoney(row.totalPayment)}</td>
                                        <td style={{ padding: "7px 10px" }}>{formatMoney(row.remainingBalance)}</td>
                                    </tr>
                                ))}
                                </tbody>
                            </table>
                        </div>
                    </div>
                )}

                {/* Review actions */}
                {canReview && (
                    <div style={{ display: "flex", flexDirection: "column", gap: 10 }}>
                        {showReject && (
                            <div style={{ display: "flex", gap: 10, alignItems: "center" }}>
                                <select value={rejectReason} onChange={(e) => setRejectReason(e.target.value)}
                                        style={{ padding: "8px 12px", borderRadius: 8, border: "1px solid #E5E7EB", fontSize: 14, outline: "none", flex: 1 }}>
                                    {REJECTION_REASONS.map((r) => <option key={r} value={r}>{r}</option>)}
                                </select>
                                <Btn variant="danger" onClick={reject} disabled={loading}>Confirm Reject</Btn>
                                <Btn variant="ghost" small onClick={() => setShowReject(false)}>Cancel</Btn>
                            </div>
                        )}
                        <div style={{ display: "flex", gap: 10 }}>
                            <Btn variant="success" onClick={approve} disabled={loading}>✓ Approve</Btn>
                            {!showReject && <Btn variant="danger" onClick={() => setShowReject(true)}>✗ Reject</Btn>}
                            <Btn variant="ghost" onClick={onClose}>Close</Btn>
                        </div>
                    </div>
                )}

                {!canReview && (
                    <div style={{ display: "flex", justifyContent: "flex-end" }}>
                        <Btn variant="ghost" onClick={onClose}>Close</Btn>
                    </div>
                )}
            </div>
        </div>
    );
}

function ConfigModal({ onClose }) {
    const [config, setConfig] = useState(null);
    const [form, setForm] = useState({});
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState(null);
    const [saved, setSaved] = useState(false);

    useEffect(() => {
        fetch(`${API}/config`).then((r) => r.json()).then((data) => {
            setConfig(data); setForm(data); setLoading(false);
        });
    }, []);

    async function save() {
        setSaving(true); setError(null); setSaved(false);
        try {
            const res = await fetch(`${API}/config`, {
                method: "PUT", headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ maxCustomerAge: parseInt(form.maxCustomerAge), baseInterestRate: parseFloat(form.baseInterestRate) }),
            });
            if (!res.ok) { const e = await res.json(); setError(e.message || "Error"); }
            else { setSaved(true); }
        } catch { setError("Network error"); }
        setSaving(false);
    }

    return (
        <div style={{ position: "fixed", inset: 0, background: "rgba(0,0,0,0.4)", display: "flex", alignItems: "center", justifyContent: "center", zIndex: 1000 }} onClick={onClose}>
            <div style={{ background: "#fff", borderRadius: 12, padding: 32, width: 400, display: "flex", flexDirection: "column", gap: 16 }} onClick={(e) => e.stopPropagation()}>
                <h2 style={{ margin: 0, fontSize: 18 }}>⚙️ Configuration</h2>
                {loading ? <div style={{ color: "#9CA3AF" }}>Loading...</div> : (
                    <>
                        {error && <div style={{ background: "#FEE2E2", color: "#991B1B", padding: "8px 12px", borderRadius: 8, fontSize: 13 }}>{error}</div>}
                        {saved && <div style={{ background: "#D1FAE5", color: "#065F46", padding: "8px 12px", borderRadius: 8, fontSize: 13 }}>Saved!</div>}
                        {[
                            { label: "Max Customer Age", field: "maxCustomerAge", type: "number" },
                            { label: "Base Interest Rate / Euribor (%)", field: "baseInterestRate", type: "number" },
                        ].map(({ label, field, type }) => (
                            <div key={field} style={{ display: "flex", flexDirection: "column", gap: 4 }}>
                                <label style={{ fontSize: 13, color: "#6B7280" }}>{label}</label>
                                <input type={type} value={form[field] || ""} onChange={(e) => setForm((f) => ({ ...f, [field]: e.target.value }))}
                                       style={{ padding: "8px 12px", borderRadius: 8, border: "1px solid #E5E7EB", fontSize: 14, outline: "none" }} />
                            </div>
                        ))}
                        <div style={{ display: "flex", gap: 8, justifyContent: "flex-end" }}>
                            <Btn variant="ghost" onClick={onClose}>Close</Btn>
                            <Btn onClick={save} disabled={saving}>{saving ? "Saving..." : "Save"}</Btn>
                        </div>
                    </>
                )}
            </div>
        </div>
    );
}

export default function App() {
    const [loans, setLoans] = useState([]);
    const [loading, setLoading] = useState(true);
    const [filter, setFilter] = useState("ALL");
    const [showModal, setShowModal] = useState(false);
    const [showConfig, setShowConfig] = useState(false);
    const [selectedLoan, setSelectedLoan] = useState(null);

    async function fetchLoans() {
        setLoading(true);
        const data = await fetch(`${API}/loans`).then((r) => r.json());
        setLoans(Array.isArray(data) ? data : []);
        setLoading(false);
    }

    async function openLoan(id) {
        const data = await fetch(`${API}/loans/${id}`).then((r) => r.json());
        setSelectedLoan(data);
    }

    useEffect(() => { fetchLoans(); }, []);

    const filtered = filter === "ALL" ? loans : loans.filter((l) => l.status === filter);

    return (
        <div style={{ width: "100%", minHeight: "100vh", background: "#F8FAFC", display: "flex", flexDirection: "column", fontFamily: "system-ui, sans-serif", boxSizing: "border-box", overflowX: "hidden" }}>
            {/* HEADER */}
            <header style={{ width: "100%", background: "#1E3A5F", color: "#fff", padding: "0 24px", display: "flex", alignItems: "center", justifyContent: "space-between", height: 60, boxSizing: "border-box" }}>
                <h1 style={{ margin: 0, fontSize: 20 }}>🏦 Loan Manager</h1>
                <div style={{ display: "flex", gap: 10 }}>
                    <button onClick={() => setShowConfig(true)} style={{ background: "transparent", color: "#fff", border: "1px solid rgba(255,255,255,0.3)", borderRadius: 8, padding: "8px 14px", cursor: "pointer", fontWeight: 600, fontSize: 14 }}>
                        ⚙️ Config
                    </button>
                    <button onClick={() => setShowModal(true)} style={{ background: "#3B82F6", color: "#fff", border: "none", borderRadius: 8, padding: "8px 16px", cursor: "pointer", fontWeight: 600 }}>
                        + New Application
                    </button>
                </div>
            </header>

            {/* MAIN */}
            <main style={{ width: "100%", padding: "24px", boxSizing: "border-box" }}>
                {/* STATS */}
                <div style={{ display: "grid", gridTemplateColumns: "repeat(4, 1fr)", gap: 16, marginBottom: 24 }}>
                    {["ALL", "IN_REVIEW", "APPROVED", "REJECTED"].map((s) => {
                        const count = s === "ALL" ? loans.length : loans.filter((l) => l.status === s).length;
                        const label = s === "ALL" ? "Total" : STATUS_COLORS[s]?.label;
                        const active = filter === s;
                        return (
                            <div key={s} onClick={() => setFilter(s)} style={{ background: active ? "#1E3A5F" : "#fff", color: active ? "#fff" : "#1F2937", borderRadius: 10, padding: "16px", cursor: "pointer", boxShadow: "0 1px 3px rgba(0,0,0,0.07)" }}>
                                <div style={{ fontSize: 28, fontWeight: 700 }}>{count}</div>
                                <div style={{ fontSize: 13 }}>{label}</div>
                            </div>
                        );
                    })}
                </div>

                {/* TABLE */}
                <div style={{ width: "100%", background: "#fff", borderRadius: 12, overflow: "hidden", boxShadow: "0 1px 3px rgba(0,0,0,0.07)" }}>
                    <table style={{ width: "100%", borderCollapse: "collapse" }}>
                        <thead>
                        <tr style={{ background: "#F3F4F6" }}>
                            {["ID", "Name", "Personal Code", "Amount", "Period", "Status", "Created", ""].map((h) => (
                                <th key={h} style={{ padding: "12px 16px", textAlign: "left", fontSize: 12, color: "#6B7280", fontWeight: 600 }}>{h}</th>
                            ))}
                        </tr>
                        </thead>
                        <tbody>
                        {loading ? (
                            <tr><td colSpan={8} style={{ padding: 40, color: "#9CA3AF", textAlign: "center" }}>Loading...</td></tr>
                        ) : filtered.length === 0 ? (
                            <tr><td colSpan={8} style={{ padding: 40, color: "#9CA3AF", textAlign: "center" }}>No data</td></tr>
                        ) : (
                            filtered.map((loan, i) => (
                                <tr key={loan.id} style={{ borderTop: "1px solid #F3F4F6", background: i % 2 === 0 ? "#fff" : "#FAFAFA" }}>
                                    <td style={{ padding: "12px 16px", fontSize: 14 }}>#{loan.id}</td>
                                    <td style={{ padding: "12px 16px", fontSize: 14 }}>{loan.firstName} {loan.lastName}</td>
                                    <td style={{ padding: "12px 16px", fontSize: 14 }}>{loan.personalCode}</td>
                                    <td style={{ padding: "12px 16px", fontSize: 14 }}>{formatMoney(loan.loanAmount)}</td>
                                    <td style={{ padding: "12px 16px", fontSize: 14 }}>{loan.loanPeriodMonths}m</td>
                                    <td style={{ padding: "12px 16px" }}><Badge status={loan.status} /></td>
                                    <td style={{ padding: "12px 16px", fontSize: 14 }}>{new Date(loan.createdAt).toLocaleDateString()}</td>
                                    <td style={{ padding: "12px 16px" }}>
                                        <button onClick={() => openLoan(loan.id)} style={{ background: "#EFF6FF", color: "#2563EB", border: "none", borderRadius: 6, padding: "5px 12px", fontSize: 12, cursor: "pointer", fontWeight: 600 }}>
                                            View
                                        </button>
                                    </td>
                                </tr>
                            ))
                        )}
                        </tbody>
                    </table>
                </div>
            </main>

            {showModal && <NewLoanModal onClose={() => setShowModal(false)} onCreated={fetchLoans} />}
            {showConfig && <ConfigModal onClose={() => setShowConfig(false)} />}
            {selectedLoan && <LoanDetailModal loan={selectedLoan} onClose={() => setSelectedLoan(null)} onUpdated={fetchLoans} />}
        </div>
    );
}