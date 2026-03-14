#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SERVER_DIR="$ROOT_DIR/chatbi-server"
REPORT_DIR="$ROOT_DIR/reports/semantic-benchmark"
SOURCE_REPORT="$SERVER_DIR/target/semantic-benchmark-report.json"
TARGET_REPORT="$REPORT_DIR/latest.json"
HISTORY_DIR="$REPORT_DIR/history"
HISTORY_FILE="$REPORT_DIR/history.jsonl"
TREND_FILE="$REPORT_DIR/trend.json"
SUMMARY_FILE="$REPORT_DIR/summary.md"
BENCHMARK_TEST="ConversationControllerTest#testSendMessage_MultiTurnSemanticBenchmarkShouldReachNinetyPercent"
MAX_DROP_PERCENT="${SEMANTIC_BENCHMARK_MAX_DROP_PERCENT:-5}"
CURRENT_BRANCH="$(git -C "$ROOT_DIR" rev-parse --abbrev-ref HEAD)"
CURRENT_COMMIT="$(git -C "$ROOT_DIR" rev-parse HEAD)"
CURRENT_COMMIT_SHORT="$(git -C "$ROOT_DIR" rev-parse --short HEAD)"
CURRENT_COMMIT_TIME="$(git -C "$ROOT_DIR" show -s --format=%cI HEAD)"

read_accuracy() {
  local file="$1"
  node -e "const fs=require('fs'); const p=process.argv[1]; const j=JSON.parse(fs.readFileSync(p,'utf8')); process.stdout.write(String(j.accuracy ?? ''));" "$file"
}

echo "======================================"
echo "ChatBI Semantic Benchmark Report"
echo "======================================"
echo "[RUN ] mvn test -Dtest=$BENCHMARK_TEST"
(cd "$SERVER_DIR" && mvn -q -Dtest="$BENCHMARK_TEST" test)

if [[ ! -f "$SOURCE_REPORT" ]]; then
  echo "[FAIL] Missing semantic benchmark report: $SOURCE_REPORT"
  exit 1
fi

mkdir -p "$REPORT_DIR"
mkdir -p "$HISTORY_DIR"
RUN_ID="$(date -u +%Y%m%dT%H%M%SZ)"

PREV_ACCURACY=""
if [[ -f "$TARGET_REPORT" ]]; then
  PREV_ACCURACY="$(read_accuracy "$TARGET_REPORT")"
fi

cp "$SOURCE_REPORT" "$TARGET_REPORT"
node -e "
const fs=require('fs');
const filePath=process.argv[1];
const branch=process.argv[2];
const commit=process.argv[3];
const commitShort=process.argv[4];
const commitTime=process.argv[5];
const runId=process.argv[6];
const maxDropPercent=Number(process.argv[7]);
const report=JSON.parse(fs.readFileSync(filePath,'utf8'));
report.metadata={
  runId,
  branch,
  commit,
  commitShort,
  commitTime,
  recordedAt: new Date().toISOString(),
  maxDropPercent
};
fs.writeFileSync(filePath, JSON.stringify(report, null, 2));
" "$TARGET_REPORT" "$CURRENT_BRANCH" "$CURRENT_COMMIT" "$CURRENT_COMMIT_SHORT" "$CURRENT_COMMIT_TIME" "$RUN_ID" "$MAX_DROP_PERCENT"
CURRENT_ACCURACY="$(read_accuracy "$TARGET_REPORT")"

cp "$TARGET_REPORT" "$HISTORY_DIR/$RUN_ID.json"
node -e "const fs=require('fs'); const p=process.argv[1]; const row=JSON.parse(fs.readFileSync(p,'utf8')); process.stdout.write(JSON.stringify(row));" "$TARGET_REPORT" >> "$HISTORY_FILE"
echo >> "$HISTORY_FILE"

node -e "
const fs=require('fs');
const historyPath=process.argv[1];
const trendPath=process.argv[2];
const summaryPath=process.argv[3];
const maxDrop=Number(process.argv[4]);
const rows=fs.readFileSync(historyPath,'utf8').split('\n').filter(Boolean).map((line)=>{
  try { return JSON.parse(line); } catch { return null; }
}).filter(Boolean);
const buildFailureBuckets=(report)=>{
  const failedCases=Array.isArray(report?.failedCases) ? report.failedCases : [];
  const buckets={
    disambiguationConflict: 0,
    secondaryMetricBinding: 0,
    timeReferenceResolution: 0,
    other: 0
  };
  const markBucket=(key)=>{
    if (Object.prototype.hasOwnProperty.call(buckets, key)) {
      buckets[key] += 1;
      return true;
    }
    return false;
  };
  for (const item of failedCases) {
    const category=typeof item === 'object' && item !== null ? String(item.category || '') : '';
    if (category && markBucket(category)) {
      continue;
    }
    const row=String(item || '');
    let matched=false;
    if (row.includes('conflict=true')) {
      matched=markBucket('disambiguationConflict');
    }
    if ((row.includes('secondary=null') || row.includes('secondary=')) && /对比|比较|一起看|跟/.test(row)) {
      matched=markBucket('secondaryMetricBinding') || matched;
    }
    if (/上周|本周|本月|上月|同比|环比|季度|去年/.test(row)) {
      matched=markBucket('timeReferenceResolution') || matched;
    }
    if (!matched) {
      markBucket('other');
    }
  }
  return {
    totalFailedCases: failedCases.length,
    buckets
  };
};
const bucketKeys=['disambiguationConflict', 'secondaryMetricBinding', 'timeReferenceResolution', 'other'];
const resolveCategory=(item)=>{
  const category=typeof item === 'object' && item !== null ? String(item.category || '') : '';
  if (category && bucketKeys.includes(category)) {
    return category;
  }
  const row=String(item || '');
  if (row.includes('conflict=true')) return 'disambiguationConflict';
  if ((row.includes('secondary=null') || row.includes('secondary=')) && /对比|比较|一起看|跟/.test(row)) return 'secondaryMetricBinding';
  if (/上周|本周|本月|上月|同比|环比|季度|去年/.test(row)) return 'timeReferenceResolution';
  return 'other';
};
const buildFailureCaseIdsByCategory=(report)=>{
  const failedCases=Array.isArray(report?.failedCases) ? report.failedCases : [];
  const caseIdsByCategory={};
  for (const key of bucketKeys) {
    caseIdsByCategory[key]=[];
  }
  for (let i=0; i<failedCases.length; i+=1) {
    const item=failedCases[i];
    const category=resolveCategory(item);
    const caseId=typeof item === 'object' && item !== null && item.caseId ? String(item.caseId) : ('legacy-' + (i + 1));
    caseIdsByCategory[category].push(caseId);
  }
  return caseIdsByCategory;
};
const buildFailureCaseIndex=(report)=>{
  const failedCases=Array.isArray(report?.failedCases) ? report.failedCases : [];
  const index={};
  for (let i=0; i<failedCases.length; i+=1) {
    const item=failedCases[i];
    const category=resolveCategory(item);
    const caseId=typeof item === 'object' && item !== null && item.caseId ? String(item.caseId) : ('legacy-' + (i + 1));
    if (typeof item === 'object' && item !== null) {
      index[caseId]={
        category,
        followupMessage: item.followupMessage || '',
        expectedConflict: item.expectedConflict,
        actualConflict: item.actualConflict,
        expectedSecondaryMetric: item.expectedSecondaryMetric,
        actualSecondaryMetric: item.actualSecondaryMetric,
        actualMetric: item.actualMetric
      };
    } else {
      index[caseId]={
        category,
        raw: String(item || '')
      };
    }
  }
  return index;
};
const buildCategoryShare=(failure)=>{
  const total=Number(failure?.totalFailedCases || 0);
  const share={};
  for (const key of bucketKeys) {
    const count=Number(failure?.buckets?.[key] || 0);
    share[key]=total > 0 ? Number(((count * 100) / total).toFixed(2)) : 0;
  }
  return share;
};
const buildCategoryDelta=(latestFailure, previousFailure)=>{
  const delta={};
  for (const key of bucketKeys) {
    const latestCount=Number(latestFailure?.buckets?.[key] || 0);
    const previousCount=Number(previousFailure?.buckets?.[key] || 0);
    delta[key]={
      countDelta: latestCount - previousCount,
      shareDelta: Number((buildCategoryShare(latestFailure)[key] - buildCategoryShare(previousFailure)[key]).toFixed(2))
    };
  }
  return delta;
};
const buildTopRegressionCategories=(delta, caseIdsByCategory, limit)=>{
  return Object.entries(delta)
    .map(([category, value])=>({
      category,
      countDelta: Number(value?.countDelta || 0),
      shareDelta: Number(value?.shareDelta || 0),
      latestCaseIds: Array.isArray(caseIdsByCategory?.[category]) ? caseIdsByCategory[category] : []
    }))
    .filter((item)=>item.countDelta > 0 || item.shareDelta > 0)
    .sort((a, b)=>{
      if (b.countDelta !== a.countDelta) return b.countDelta - a.countDelta;
      return b.shareDelta - a.shareDelta;
    })
    .slice(0, limit);
};
const buildCategoryHistoryWindow=(rows, windowSize)=>{
  const latestRows=rows.slice(-windowSize);
  return latestRows.map((row)=>{
    const failure=buildFailureBuckets(row);
    const meta=row?.metadata || {};
    return {
      runId: meta.runId || 'N/A',
      branch: meta.branch || 'N/A',
      commit: meta.commitShort || meta.commit || 'N/A',
      buckets: failure.buckets
    };
  });
};
const buildTrendLine=(historyWindow, key)=>{
  if (!historyWindow.length) return 'N/A';
  return historyWindow.map((item)=>String(Number(item?.buckets?.[key] || 0))).join(' -> ');
};
const buildCategoryAlerts=(categoryDelta, threshold)=>{
  const alerts=[];
  for (const key of bucketKeys) {
    const countDelta=Math.abs(Number(categoryDelta?.[key]?.countDelta || 0));
    if (countDelta > threshold) {
      alerts.push({
        category: key,
        countDelta: Number(categoryDelta[key].countDelta || 0),
        threshold
      });
    }
  }
  return alerts;
};
const accuracyRows=rows.map((r)=>Number(r.accuracy)).filter(Number.isFinite);
const avg=accuracyRows.length?accuracyRows.reduce((a,b)=>a+b,0)/accuracyRows.length:0;
const latest=rows.at(-1) || null;
const previous=rows.length>1 ? rows.at(-2) : null;
const latestAcc=Number(latest?.accuracy ?? NaN);
const prevAcc=Number(previous?.accuracy ?? NaN);
const drop=(Number.isFinite(latestAcc) && Number.isFinite(prevAcc)) ? Number((prevAcc-latestAcc).toFixed(2)) : 0;
const regression=(Number.isFinite(drop) && drop > maxDrop);
const latestFailure=buildFailureBuckets(latest);
const previousFailure=buildFailureBuckets(previous);
const latestFailureCaseIds=buildFailureCaseIdsByCategory(latest);
const previousFailureCaseIds=buildFailureCaseIdsByCategory(previous);
const latestFailureCaseIndex=buildFailureCaseIndex(latest);
const previousFailureCaseIndex=buildFailureCaseIndex(previous);
const latestCategoryShare=buildCategoryShare(latestFailure);
const previousCategoryShare=buildCategoryShare(previousFailure);
const categoryDelta=buildCategoryDelta(latestFailure, previousFailure);
const topRegressionCategories=buildTopRegressionCategories(categoryDelta, latestFailureCaseIds, 3);
const categoryHistoryWindow=buildCategoryHistoryWindow(rows, 5);
const categoryAlertThreshold=1;
const categoryAlerts=buildCategoryAlerts(categoryDelta, categoryAlertThreshold);
const buildCaseDetailLine=(caseId, detail)=>{
  if (!detail) return '- ' + caseId + ': detail-missing';
  if (typeof detail.raw === 'string') {
    const raw=detail.raw.length > 120 ? (detail.raw.slice(0, 117) + '...') : detail.raw;
    return '- ' + caseId + ': raw=' + raw;
  }
  const followup=String(detail.followupMessage || '').replace(/\s+/g, ' ').slice(0, 48);
  return '- ' + caseId
    + ': followup=' + followup
    + '; conflict=' + String(detail.expectedConflict) + '->' + String(detail.actualConflict)
    + '; secondary=' + String(detail.expectedSecondaryMetric ?? 'null') + '->' + String(detail.actualSecondaryMetric ?? 'null')
    + '; metric=' + String(detail.actualMetric ?? 'null');
};
const trend={
  samples: rows.length,
  latest,
  previous,
  minAccuracy: accuracyRows.length?Math.min(...accuracyRows):0,
  maxAccuracy: accuracyRows.length?Math.max(...accuracyRows):0,
  avgAccuracy: Number(avg.toFixed(2)),
  maxAllowedDrop: maxDrop,
  regression,
  latestFailure,
  previousFailure,
  latestFailureCaseIds,
  previousFailureCaseIds,
  latestFailureCaseIndex,
  previousFailureCaseIndex,
  latestCategoryShare,
  previousCategoryShare,
  categoryDelta,
  topRegressionCategories,
  categoryHistoryWindow,
  categoryAlertThreshold,
  categoryAlerts
};
fs.writeFileSync(trendPath, JSON.stringify(trend, null, 2));
const latestMeta=latest?.metadata || {};
const previousMeta=previous?.metadata || {};
const lines=[
  '# Semantic Benchmark Summary',
  '',
  '- Samples: ' + trend.samples,
  '- Latest Accuracy: ' + (Number.isFinite(latestAcc) ? latestAcc : 'N/A') + '%',
  '- Previous Accuracy: ' + (Number.isFinite(prevAcc) ? prevAcc : 'N/A') + '%',
  '- Accuracy Drop: ' + drop + '%',
  '- Max Allowed Drop: ' + maxDrop + '%',
  '- Regression Guard: ' + (regression ? 'TRIGGERED' : 'PASS'),
  '',
  '## Failure Buckets (Latest)',
  '- Total Failed Cases: ' + latestFailure.totalFailedCases,
  '- Disambiguation Conflict: ' + latestFailure.buckets.disambiguationConflict,
  '- Secondary Metric Binding: ' + latestFailure.buckets.secondaryMetricBinding,
  '- Time Reference Resolution: ' + latestFailure.buckets.timeReferenceResolution,
  '- Other: ' + latestFailure.buckets.other,
  '',
  '## Failure Buckets (Previous)',
  '- Total Failed Cases: ' + previousFailure.totalFailedCases,
  '- Disambiguation Conflict: ' + previousFailure.buckets.disambiguationConflict,
  '- Secondary Metric Binding: ' + previousFailure.buckets.secondaryMetricBinding,
  '- Time Reference Resolution: ' + previousFailure.buckets.timeReferenceResolution,
  '- Other: ' + previousFailure.buckets.other,
  '',
  '## Category Share Delta (%)',
  '- Disambiguation Conflict: latest=' + latestCategoryShare.disambiguationConflict + '%, previous=' + previousCategoryShare.disambiguationConflict + '%, delta=' + categoryDelta.disambiguationConflict.shareDelta + '% (countDelta=' + categoryDelta.disambiguationConflict.countDelta + ')',
  '- Secondary Metric Binding: latest=' + latestCategoryShare.secondaryMetricBinding + '%, previous=' + previousCategoryShare.secondaryMetricBinding + '%, delta=' + categoryDelta.secondaryMetricBinding.shareDelta + '% (countDelta=' + categoryDelta.secondaryMetricBinding.countDelta + ')',
  '- Time Reference Resolution: latest=' + latestCategoryShare.timeReferenceResolution + '%, previous=' + previousCategoryShare.timeReferenceResolution + '%, delta=' + categoryDelta.timeReferenceResolution.shareDelta + '% (countDelta=' + categoryDelta.timeReferenceResolution.countDelta + ')',
  '- Other: latest=' + latestCategoryShare.other + '%, previous=' + previousCategoryShare.other + '%, delta=' + categoryDelta.other.shareDelta + '% (countDelta=' + categoryDelta.other.countDelta + ')',
  '',
  '## Top Regression Categories',
  ...(topRegressionCategories.length ? topRegressionCategories.map((item, index)=>'- Top' + (index + 1) + ': ' + item.category + ' (countDelta=' + item.countDelta + ', shareDelta=' + item.shareDelta + '%, caseIds=' + (item.latestCaseIds.length ? item.latestCaseIds.join(',') : 'none') + ')') : ['- None']),
  '',
  '## Top Regression Category Details',
  ...(topRegressionCategories.length
    ? topRegressionCategories.flatMap((item, index)=>{
      const title='### Top' + (index + 1) + ' ' + item.category;
      const ids=item.latestCaseIds.slice(0, 3);
      const detailLines=ids.length
        ? ids.map((id)=>buildCaseDetailLine(id, latestFailureCaseIndex[id]))
        : ['- none'];
      return [title, ...detailLines];
    })
    : ['- None']),
  '',
  '## Category Trend (Last 5 Runs)',
  '- Disambiguation Conflict: ' + buildTrendLine(categoryHistoryWindow, 'disambiguationConflict'),
  '- Secondary Metric Binding: ' + buildTrendLine(categoryHistoryWindow, 'secondaryMetricBinding'),
  '- Time Reference Resolution: ' + buildTrendLine(categoryHistoryWindow, 'timeReferenceResolution'),
  '- Other: ' + buildTrendLine(categoryHistoryWindow, 'other'),
  '',
  '## Category Alerts',
  ...(categoryAlerts.length
    ? categoryAlerts.map((item)=>'- ALERT: ' + item.category + ' countDelta=' + item.countDelta + ' (threshold=' + item.threshold + ')')
    : ['- PASS: no category delta exceeds threshold (' + categoryAlertThreshold + ')']),
  '',
  '## Latest Build Metadata',
  '- Branch: ' + (latestMeta.branch || 'N/A'),
  '- Commit: ' + (latestMeta.commitShort || latestMeta.commit || 'N/A'),
  '- Commit Time: ' + (latestMeta.commitTime || 'N/A'),
  '- Report Generated At: ' + (latest?.generatedAt || 'N/A'),
  '',
  '## Previous Build Metadata',
  '- Branch: ' + (previousMeta.branch || 'N/A'),
  '- Commit: ' + (previousMeta.commitShort || previousMeta.commit || 'N/A'),
  '- Commit Time: ' + (previousMeta.commitTime || 'N/A'),
  '- Report Generated At: ' + (previous?.generatedAt || 'N/A')
];
fs.writeFileSync(summaryPath, lines.join('\n') + '\n');
" "$HISTORY_FILE" "$TREND_FILE" "$SUMMARY_FILE" "$MAX_DROP_PERCENT"

if [[ -n "$PREV_ACCURACY" ]]; then
  node -e "
const current=Number(process.argv[1]);
const previous=Number(process.argv[2]);
const maxDrop=Number(process.argv[3]);
if (!Number.isFinite(current) || !Number.isFinite(previous) || !Number.isFinite(maxDrop)) process.exit(0);
const drop=Number((previous-current).toFixed(2));
if (drop > maxDrop) {
  console.error('[FAIL] Semantic benchmark regressed: previous=' + previous + ', current=' + current + ', drop=' + drop + ' > maxDrop=' + maxDrop);
  process.exit(1);
}
console.log('[ OK ] Semantic benchmark trend: previous=' + previous + ', current=' + current + ', drop=' + drop + ', maxDrop=' + maxDrop);
" "$CURRENT_ACCURACY" "$PREV_ACCURACY" "$MAX_DROP_PERCENT"
else
  echo "[ OK ] Semantic benchmark trend: first baseline sample (current=$CURRENT_ACCURACY)"
fi

echo "[ OK ] Semantic benchmark report generated: ${TARGET_REPORT#$ROOT_DIR/}"
echo "[ OK ] Semantic benchmark history appended: ${HISTORY_FILE#$ROOT_DIR/}"
echo "[ OK ] Semantic benchmark trend generated: ${TREND_FILE#$ROOT_DIR/}"
echo "[ OK ] Semantic benchmark summary generated: ${SUMMARY_FILE#$ROOT_DIR/}"
echo "======================================"
echo "Semantic benchmark report passed"
echo "======================================"
