import { useState } from "react";

export default function NodeInspector({ node, explanations, onClose }) {
  const [aiText, setAiText] = useState(null);
  const [loading, setLoading] = useState(false);
  const [closing, setClosing] = useState(false);
  const [activeSlide, setActiveSlide] = useState("shap");

  if (!node) return null;
  console.log("Selected Node:", node);
  const isAnomalous = node.is_anomalous === 1;
  const reasons = explanations?.[String(node.id)]?.reasons || [];

  const generateAIExplanation = async () => {
    setLoading(true);

    setTimeout(() => {
      setAiText(
        `Account ${node.id} shows unusual transaction behavior with high connectivity 
to anomalous accounts. Rapid inflow and outflow patterns indicate potential mule activity.`
      );
      setLoading(false);
    }, 1200);
  };
  function ShapSlide({ reasons, isAnomalous }) {
    if (!reasons.length) {
      return (
        <p className="text-xs text-gray-500 italic">
          No strong SHAP signals for this account.
        </p>
      );
    }

    return (
      <ul className="space-y-2">
        {reasons.map((reason, i) => (
          <li
            key={i}
            className={`flex gap-2 items-start p-2 rounded-md text-sm
            ${
              isAnomalous
                ? "bg-red-950/40 text-red-200"
                : "bg-green-950/40 text-green-200"
            }`}
          >
            <span className="mt-0.5">▸</span>
            <span>{reason}</span>
          </li>
        ))}
      </ul>
    );
  }
  function AISlide({ aiText, loading, onGenerate }) {
    return (
      <>
        <div className="min-h-20 mb-3">
          {aiText ? (
            <div className="bg-zinc-800 p-3 rounded-md text-sm text-gray-200">
              {aiText}
            </div>
          ) : (
            <p className="text-xs text-gray-500 italic">
              Generate a natural language explanation based on model signals.
            </p>
          )}
        </div>

        <button
          onClick={onGenerate}
          disabled={loading}
          className="w-full rounded-md bg-white text-black py-2 text-sm font-medium hover:bg-gray-200"
        >
          {loading ? "Generating..." : "Generate AI Summary"}
        </button>
      </>
    );
  }

  const handleClose = () => {
    setClosing(true);

    setTimeout(() => {
      setAiText(null);
      setClosing(false);
      onClose();
    }, 250);
  };

  return (
    <aside
      className={`fixed right-0 top-0 pt-10 z-100 w-95 h-screen overflow-y-auto animate-slide-in
    ${
      isAnomalous
        ? "bg-zinc-900 border-l border-red-600 shadow-[0_0_20px_rgba(239,68,68,0.4)]"
        : "bg-zinc-900 border-l border-green-600 shadow-[0_0_20px_rgba(34,197,94,0.4)]"
    }
  `}
    >
      {/* Header */}
      <div className="flex justify-between">
        <h2 className="text-lg font-semibold pl-5">
          Node Forensics:{" "}
          <span className={isAnomalous ? "text-red-400" : "text-green-400"}>
            ACC{node.id}
          </span>
        </h2>
        <button
          onClick={handleClose}
          className="text-gray-400 hover:text-white pr-6 cursor-pointer"
        >
          ✕
        </button>
      </div>

      {/* Account Summary */}
      <Section title="Account Summary">
        <Metric
          label="Risk Status"
          value={node.is_anomalous === 1 ? "Anomalous" : "Normal"}
          highlight={node.is_anomalous === 1}
        />

        <Metric label="Risk Score" value={(node.height * 100).toFixed(1)} />
      </Section>

      {/* Metrics */}
      <Section title="Metrics">
        <Metric label="Total Transactions" value={Math.round(node.size)} />

        <Metric label="Suspicious vs Normal" value="40 / 160" />
        <Metric
          label="Connectivity Score"
          value="92"
          color={isAnomalous ? "red" : "green"}
        />
      </Section>

      {/* Explainability */}
      <Section title="Explainability">
        {/* Tabs */}
        <div className="flex mb-4 rounded-md overflow-hidden border border-zinc-700">
          <button
            onClick={() => setActiveSlide("shap")}
            className={`flex-1 py-2 text-sm font-medium
        ${
          activeSlide === "shap"
            ? "bg-zinc-800 text-white"
            : "bg-zinc-900 text-gray-400 hover:text-white"
        }`}
          >
            SHAP Explainability
          </button>

          <button
            onClick={() => setActiveSlide("ai")}
            className={`flex-1 py-2 text-sm font-medium
        ${
          activeSlide === "ai"
            ? "bg-zinc-800 text-white"
            : "bg-zinc-900 text-gray-400 hover:text-white"
        }`}
          >
            AI Explanation
          </button>
        </div>

        {/* Slide Content */}
        <div className="min-h-[140px] transition-all">
          {activeSlide === "shap" ? (
            <ShapSlide reasons={reasons} isAnomalous={isAnomalous} />
          ) : (
            <AISlide
              aiText={aiText}
              loading={loading}
              onGenerate={generateAIExplanation}
            />
          )}
        </div>
      </Section>

      {/* Actions */}
      <div className="flex gap-3 p-5">
        <button className="flex-1 rounded-md border border-green-500 text-green-400 py-2">
          Mark as Safe
        </button>
        <button className="flex-1 rounded-md bg-red-600 py-2">
          Initiate Freeze
        </button>
      </div>
    </aside>
  );
}

function Section({ title, children }) {
  return (
    <div className="p-5 border-b border-zinc-800">
      <h3 className="mb-3 text-sm font-semibold uppercase text-gray-400">
        {title}
      </h3>
      {children}
    </div>
  );
}

function Metric({ label, value, highlight, color }) {
  const colorClass =
    color === "red"
      ? "text-red-400"
      : color === "green"
      ? "text-green-400"
      : "";

  return (
    <div className="flex justify-between text-sm mb-2">
      <span className="text-gray-400">{label}</span>
      <span className={`${highlight ? "font-semibold" : ""} ${colorClass}`}>
        {value}
      </span>
    </div>
  );
}
