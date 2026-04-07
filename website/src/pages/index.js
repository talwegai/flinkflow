import clsx from 'clsx';
import Link from '@docusaurus/Link';
import useDocusaurusContext from '@docusaurus/useDocusaurusContext';
import Layout from '@theme/Layout';
import HomepageFeatures from '@site/src/components/HomepageFeatures';

import Heading from '@theme/Heading';
import styles from './index.module.css';

function HomepageHeader() {
  const {siteConfig} = useDocusaurusContext();
  return (
    <header className={clsx('hero', styles.heroBanner)}>
      <div className="container">
        <Heading as="h1" className="hero__title">
          {siteConfig.title}
        </Heading>
        <p className="hero__subtitle">{siteConfig.tagline}</p>
        <div className={styles.buttons}>
          <Link
            className="button button--secondary button--lg"
            to="/docs/">
            Get Started with Flinkflow ⚡
          </Link>
        </div>
      </div>
    </header>
  );
}

export default function Home() {
  const {siteConfig} = useDocusaurusContext();
  return (
    <Layout
      title={`Flinkflow | ${siteConfig.tagline}`}
      description="Declarative, low-code data streaming platform built on top of Apache Flink.">
      <HomepageHeader />
      <main>
        <div className="container" style={{padding: '4rem 0', textAlign: 'center'}}>
           <h2>Democratizing Data Engineering</h2>
           <p style={{fontSize: '1.2rem', maxWidth: '800px', margin: '0 auto'}}>
             Flinkflow abstracts the complexities of the Flink API into a simple, Kubernetes-native YAML DSL, 
             enabling Analysts and DevOps teams to build massive-scale streaming pipelines in seconds.
           </p>
        </div>
      </main>
    </Layout>
  );
}
