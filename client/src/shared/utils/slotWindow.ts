// A slot's window crosses the wire as two absolute instants (ISO-8601, UTC), because an
// interview is an appointment between people who may sit in different zones. The forms and
// tables here speak the local wall clock, so this module is the single place where the two
// meet: the browser's own zone is the one the user is reading and typing in.

const pad = (value: number): string => String(value).padStart(2, '0');

/** Local calendar date of an instant, as YYYY-MM-DD. */
export const toLocalDate = (instant: string): string => {
    const at = new Date(instant);
    return `${at.getFullYear()}-${pad(at.getMonth() + 1)}-${pad(at.getDate())}`;
};

/** Local wall-clock time of an instant, as HH:mm:ss. */
export const toLocalTime = (instant: string): string => {
    const at = new Date(instant);
    return `${pad(at.getHours())}:${pad(at.getMinutes())}:${pad(at.getSeconds())}`;
};

/**
 * Turns a local date + time (as the <input type="date"> and <input type="time"> pair yields
 * them) into the instant they name here. A date-time string without an offset is parsed in
 * the runtime's zone, which is exactly the reading the user intended.
 */
export const toInstant = (date: string, time: string): string =>
    new Date(`${date}T${time.length === 5 ? `${time}:00` : time}`).toISOString();
