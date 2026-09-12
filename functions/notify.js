export async function onRequestPost(context) {
  const { request, env } = context;

  let body;
  try {
    body = await request.json();
  } catch {
    return new Response(JSON.stringify({ error: "Invalid request" }), { status: 400 });
  }

  const email = (body.email || "").trim();
  const validEmail = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  if (!validEmail.test(email)) {
    return new Response(JSON.stringify({ error: "Invalid email" }), { status: 400 });
  }

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
    // 1) Notify B Sculpt
    await sendEmail({
      from: "B Sculpt Website <notify@bsculpt.com.au>",
      to: ["info@bsculpt.com.au"],
      subject: "Website lead",
      text: `New lead from the coming-soon page.\n\nEmail: ${email}`,
    });

    // 2) Confirm to the visitor
    await sendEmail({
      from: "B Sculpt <notify@bsculpt.com.au>",
      to: [email],
      subject: "Thanks for your interest — B Sculpt",
      text: `Thank you for your interest in B Sculpt.\n\nWe'll be in touch the moment we open our doors.\n\nWarm regards,\nSonya @ B Sculpt\n158 Osborne Park, Perth`,
    });

    return new Response(JSON.stringify({ ok: true }), { status: 200 });
  } catch (err) {
    return new Response(JSON.stringify({ error: "Failed to send" }), { status: 500 });
  }
}
