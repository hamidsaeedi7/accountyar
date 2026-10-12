import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import path from 'node:path';
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const result = spawnSync('java', ['-cp', 'build/core-tests', 'DateDump'], { cwd: root, encoding: 'utf8' });
if (result.error || result.status !== 0) {
  console.error('Run ./scripts/test-core.sh first, with JDK 17 installed.');
  console.error(result.error?.message || result.stderr);
  process.exit(1);
}
const formatter = new Intl.DateTimeFormat('en-u-ca-persian', {
  timeZone: 'UTC', year: 'numeric', month: 'numeric', day: 'numeric'
});
let count = 0;
const mismatches = [];
for (const row of result.stdout.trim().split('\n')) {
  const [date, year, month, day] = row.trim().split(',');
  const parts = formatter.formatToParts(new Date(`${date}T12:00:00Z`));
  const value = (type) => Number(parts.find((part) => part.type === type).value);
  if (value('year') !== Number(year) || value('month') !== Number(month) || value('day') !== Number(day)) {
    mismatches.push(row);
  }
  count++;
}
console.log(`Checked ${count} dates against ICU/Intl Persian calendar; mismatches: ${mismatches.length}.`);
if (mismatches.length) {
  console.error(mismatches.slice(0, 10).join('\n'));
  process.exit(1);
}
