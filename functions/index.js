const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const nodemailer = require("nodemailer");

const transporter = nodemailer.createTransport({
  service: "gmail",
  auth: {
    user: process.env.GMAIL_USER,
    pass: process.env.GMAIL_PASS,
  },
});

exports.sendMail = onDocumentCreated("mail/{docId}", async (event) => {
  const data = event.data.data();
  if (!data || !data.to || !data.message) return;

  await transporter.sendMail({
    from: `"SMS 匿名投票" <${process.env.GMAIL_USER}>`,
    to: Array.isArray(data.to) ? data.to.join(",") : data.to,
    subject: data.message.subject,
    html: data.message.html,
  });
});
