import styles from "./login.module.css";

export default function LoginSplitLayout({ branding, children, footer }) {
  return (
    <main className={styles.root} data-login-page>
      <div className={styles.noise} aria-hidden />
      <div className={styles.grid}>
        <aside className={styles.left}>{branding}</aside>
        <div className={styles.right}>
          {children}
          {footer}
        </div>
      </div>
    </main>
  );
}
