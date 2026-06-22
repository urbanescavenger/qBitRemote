// Shared qBittorrent WebUI API helper.
// qBittorrent expects POST /api/v2/auth/login with
// Content-Type: application/x-www-form-urlencoded and body
// username=...&password=...; on success it returns "Ok." and sets a SID cookie
// that must be sent on subsequent requests. credentials:'include' makes RN's
// networking layer (OkHttp on Android) keep and send that cookie.

export interface QbLoginResult {
  ok: boolean;
  status?: number;
  body?: string;
  error?: string;
}

export function qbBaseUrl(s: any): string {
  const scheme = s.ssl === 'true' || s.ssl === true ? 'https://' : 'http://';
  return `${scheme}${s.host}:${s.port}`;
}

export async function qbLogin(s: any): Promise<QbLoginResult> {
  const body = `username=${encodeURIComponent(s.username ?? '')}&password=${encodeURIComponent(s.password ?? '')}`;
  try {
    const res = await fetch(`${qbBaseUrl(s)}/api/v2/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      credentials: 'include',
      body,
    });
    const text = (await res.text()).trim();
    // qBittorrent version-dependent success responses:
    //  - newer versions: HTTP 204 No Content (login action returns no body)
    //  - older versions: HTTP 200 with body "Ok."
    // Failures are 401 (bad creds) / 403 (IP banned) — surfaced via {status,body} below.
    const ok = res.status === 204
      || (res.status === 200 && text.toLowerCase() === 'ok.');
    return { ok, status: res.status, body: text };
  } catch (e: any) {
    console.log('qbLogin error', e);
    return { ok: false, error: e?.message ?? String(e) };
  }
}

export async function qbGet<T = any>(s: any, path: string): Promise<T | null> {
  try {
    const res = await fetch(`${qbBaseUrl(s)}${path}`, { credentials: 'include' });
    return await res.json();
  } catch (e) {
    console.log('qbGet error', e);
    return null;
  }
}

export async function qbPost(s: any, path: string, body: FormData): Promise<string | null> {
  try {
    const res = await fetch(`${qbBaseUrl(s)}${path}`, {
      method: 'POST',
      credentials: 'include',
      body,
    });
    return await res.text();
  } catch (e) {
    console.log('qbPost error', e);
    return null;
  }
}

// Run a control action (stop/start/recheck/delete, ...) as POST with the params
// urlencoded in the body (qBittorrent reads POST params from the body, not the
// query). Carries the SID cookie (credentials:'include') and reports success the
// same way as login/add: 204 (newer qBittorrent) or 200 "Ok." (older).
export async function qbAction(
  s: any,
  path: string,
  params: Record<string, string> = {}
): Promise<QbLoginResult> {
  const body = Object.entries(params)
    .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(v)}`)
    .join('&');
  try {
    const res = await fetch(`${qbBaseUrl(s)}${path}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      credentials: 'include',
      body,
    });
    const text = (await res.text()).trim();
    const ok = res.status === 204
      || (res.status === 200 && text.toLowerCase() === 'ok.');
    return { ok, status: res.status, body: text };
  } catch (e: any) {
    console.log('qbAction error', e);
    return { ok: false, error: e?.message ?? String(e) };
  }
}