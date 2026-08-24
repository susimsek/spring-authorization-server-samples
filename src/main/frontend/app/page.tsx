import Link from "next/link";
import { AdminPostLoginRedirect } from "@/components/admin/AdminPostLoginRedirect";

export default function HomePage() {
  return (
    <>
      <AdminPostLoginRedirect />
      <main className="min-vh-100 d-flex align-items-center justify-content-center bg-body-tertiary">
        <Link className="btn btn-primary" href="/en/login">
          Continue
        </Link>
      </main>
    </>
  );
}
