/** Today's date as YYYY-MM-DD in local time, for <input type="date"> bounds. */
export function todayIso() {
  const now = new Date()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  return `${now.getFullYear()}-${month}-${day}`
}

/**
 * Age in whole years for a YYYY-MM-DD birth date. Only a UI hint for showing
 * the guardian section: the backend computes majority itself and has the last word.
 */
export function ageOn(birthDateIso, todayIsoDate = todayIso()) {
  const [by, bm, bd] = birthDateIso.split('-').map(Number)
  const [ty, tm, td] = todayIsoDate.split('-').map(Number)
  const hadBirthday = tm > bm || (tm === bm && td >= bd)
  return ty - by - (hadBirthday ? 0 : 1)
}

export function isMinor(birthDateIso) {
  return Boolean(birthDateIso) && ageOn(birthDateIso) < 18
}
