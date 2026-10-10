// functions/contact.js — Cloudflare Pages Function for the website contact form (POST /contact).
// Sits beside functions/notify.js (unchanged). Uses the same RESEND_API_KEY environment variable
// and the notify@bsculpt.com.au sender that notify.js already uses.

const MAX = { name: 100, email: 200, phone: 40, message: 3000 };

function json(obj, status) {
  return new Response(JSON.stringify(obj), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

function clean(v, max) {
  return String(v == null ? "" : v).replace(/\r\n/g, "\n").trim().slice(0, max);
}

export async function onRequestPost(context) {
  const { request, env } = context;

  let body;
  try {
    body = await request.json();
  } catch {
    return json({ error: "Invalid request" }, 400);
  }

  // Honeypot: real visitors never fill this hidden field. Pretend success, send nothing.
  if (body.company) return json({ ok: true }, 200);

  const name = clean(body.name, MAX.name).replace(/\s+/g, " ");
  const email = clean(body.email, MAX.email);
  const phone = clean(body.phone, MAX.phone);
  const message = clean(body.message, MAX.message);

  if (!name) return json({ error: "Name required" }, 400);
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) return json({ error: "Invalid email" }, 400);
  if (!message) return json({ error: "Message required" }, 400);

  const RESEND_API_KEY = env.RESEND_API_KEY;

  async function sendEmail(payload) {
    const res = await fetch("https://api.resend.com/emails", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Authorization": `Bearer ${RESEND_API_KEY}`,
      },
      body: JSON.stringify(payload),
    });
    if (!res.ok) {
      const errText = await res.text();
      throw new Error(`Resend error: ${errText}`);
    }
  }

  try {
    // 1) Notify B Sculpt (info@ — the dashboard flags info@ emails as leads)
    await sendEmail({
      from: "B Sculpt Website <notify@bsculpt.com.au>",
      to: ["info@bsculpt.com.au"],
      reply_to: email,
      subject: `Website enquiry – ${name}`,
      text:
        `New enquiry from the website contact form.\n\n` +
        `Name: ${name}\n` +
        `Email: ${email}\n` +
        `Phone: ${phone || "-"}\n\n` +
        `Message:\n${message}`,
    });

    // 2) Confirm to the visitor
    await sendEmail({
      from: "B Sculpt <notify@bsculpt.com.au>",
      to: [email],
      subject: "Thank you for your interest in B Sculpt",
      text:
        `Thank you for your interest in B Sculpt.\n\n` +
        `We'll be in touch shortly and will happily discuss your goals and our services.\n\n` +
        `Warm regards,\n` +
        `Sonya @ B Sculpt\n` +
        `158 Main Street, Osborne Park, Perth 6017`,
    });

    return json({ ok: true }, 200);
  } catch (err) {
    return json({ error: "Failed to send" }, 500);
  }
}
