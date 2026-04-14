import { Link } from 'react-router-dom'
import { useElegantAnimations } from '../hooks/useElegantAnimations'

const flowSteps = [
  {
    step: '01',
    title: 'Create access',
    description: 'Start with sign up or log in so the workspace stays gated.',
  },
  {
    step: '02',
    title: 'Connect data',
    description: 'Add the datasource details once before anything else unlocks.',
  },
  {
    step: '03',
    title: 'Run queries',
    description: 'Use the workspace only after access and setup are complete.',
  },
]

const productSignals = [
  'Schema-aware query building',
  'Guided datasource onboarding',
  'JSON request playground',
]

export default function LandingPage() {
  const rootRef = useElegantAnimations<HTMLDivElement>([])

  return (
    <main
      ref={rootRef}
      className="page-shell flex h-[100dvh] items-center overflow-hidden py-4 sm:py-5"
      data-animate="scene"
    >
      <section className="surface-panel mx-auto w-full max-w-[min(92vw,1280px)] px-5 py-6 sm:px-8 sm:py-7 lg:px-10">
        <div
          className="absolute left-[-3rem] top-8 h-28 w-28 rounded-full bg-[radial-gradient(circle,_rgba(255,171,115,0.58),_transparent_72%)] blur-3xl"
          data-float="slow"
          data-glow="pulse"
        />
        <div
          className="absolute right-4 top-10 h-24 w-24 rounded-full bg-[radial-gradient(circle,_rgba(82,215,255,0.46),_transparent_72%)] blur-3xl"
          data-float="medium"
        />

        <div className="relative z-10">
          <span className="section-badge" data-animate="chip">
            Production-ready data console
          </span>

          <div className="mt-6 grid gap-6 lg:grid-cols-[1.12fr_0.88fr] lg:items-center">
            <div>
              <p className="text-xs uppercase tracking-[0.32em] text-zinc-500">
                ForgeQL
              </p>
              <h1
                className="display-title mt-4 max-w-[11ch] text-[2.8rem] text-white sm:text-[3.8rem] lg:text-[4.7rem]"
                data-animate="hero"
              >
                Explore PostgreSQL with a premium runtime console.
              </h1>
              <p
                className="display-copy mt-4 max-w-xl text-sm sm:text-[0.96rem]"
                data-animate="hero"
              >
                ForgeQL turns live PostgreSQL schemas into a controlled interface for
                discovery, aggregates, and row-level operations without exposing raw
                SQL.
              </p>

              <div className="mt-6 flex flex-wrap gap-3">
                <Link
                  to="/login"
                  className="secondary-button min-w-[148px]"
                  data-pressable
                >
                  Log in
                </Link>
                <Link
                  to="/signup"
                  className="primary-button min-w-[148px]"
                  data-pressable
                  data-glow="pulse"
                >
                  Sign up
                </Link>
              </div>
            </div>

            <div className="grid gap-3">
              {flowSteps.map((item) => (
                <article
                  key={item.step}
                  className="surface-card flex items-start gap-4 p-4"
                  data-animate="panel"
                  data-tilt
                >
                  <span className="flex h-11 w-11 shrink-0 items-center justify-center rounded-2xl border border-cyan-300/18 bg-cyan-300/10 text-sm font-semibold text-cyan-100">
                    {item.step}
                  </span>
                  <div>
                    <h2 className="text-base font-semibold text-white">
                      {item.title}
                    </h2>
                    <p className="mt-2 text-sm leading-6 text-zinc-400">
                      {item.description}
                    </p>
                  </div>
                </article>
              ))}
            </div>
          </div>

          <div className="subtle-divider mt-6" data-animate="line" />

          <div className="mt-6 grid gap-3 md:grid-cols-3">
            {productSignals.map((signal) => (
              <div
                key={signal}
                className="rounded-[22px] border border-white/8 bg-white/[0.03] px-4 py-3 text-sm text-zinc-300"
                data-animate="panel"
              >
                {signal}
              </div>
            ))}
          </div>
        </div>
      </section>
    </main>
  )
}
