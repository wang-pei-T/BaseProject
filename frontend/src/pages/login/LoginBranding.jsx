import appConfig from "../../config/app-config";
import styles from "./login.module.css";

export default function LoginBranding() {
  const b = appConfig.branding || {};
  return (
    <div className={styles.brandStage}>
      <div className={styles.brandAmbient} aria-hidden>
        <span className={styles.brandOrbA} />
        <span className={styles.brandOrbB} />
        <span className={styles.brandOrbC} />
        <div className={styles.brandMesh} />
        <div className={styles.brandArcWrap}>
          <svg className={styles.brandArcSvg} viewBox="0 0 400 400" xmlns="http://www.w3.org/2000/svg">
            <circle cx="200" cy="200" r="172" fill="none" stroke="currentColor" strokeWidth="1.2" strokeDasharray="44 30" />
            <circle cx="200" cy="200" r="146" fill="none" stroke="currentColor" strokeWidth="0.6" strokeDasharray="6 14" opacity="0.65" />
          </svg>
        </div>
      </div>
      <div className={styles.brandContent}>
        <div className={styles.brandRail} aria-hidden />
        <div className={styles.brandInner}>
          <div className={styles.brandLockup}>
            <div className={styles.brandLogoRow}>
              <span className={styles.brandLogoGlow} />
              <img className={styles.brandLogo} src={b.logoUrl || "/favicon.svg"} alt={b.logoAlt || "logo"} />
            </div>
            <div className={styles.brandHeadlineBlock}>
              <h1 className={styles.brandTitle}>{b.sidebarTitle || appConfig.pageTitle}</h1>
            </div>
          </div>
          <p className={styles.brandSlogan}>统一身份与多租户治理，安全登录后进入工作台。</p>
        </div>
      </div>
    </div>
  );
}
