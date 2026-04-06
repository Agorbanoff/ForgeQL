import type { ReactNode } from 'react'
import { useElegantAnimations } from '../hooks/useElegantAnimations'

type AuthLayoutProps = {
  badge: string
  title: string
  description: string
  formTitle: string
  formDescription: string
  form: ReactNode
  footer: ReactNode
}

const showcasePoints = [
  'Schema-aware query composition',
  'Secure datasource onboarding',
  'Elegant JSON-to-SQL workflows',
]

const featureCards = [
  {
    title: 'Structured Query Control',
    copy: 'Compose rich request payloads with filters, limits, and nested relations without losing readability.',
  },
  {
    title: 'Fast Setup Paths',
    copy: 'Move from account creation to datasource wiring in one smooth path designed for demos and real projects.',
  },
  {
    title: 'Calm Operational Surface',
    copy: 'Use a focused workspace with clear states, subtle cues, and motion that supports the flow instead of distracting from it.',
  },
]

const stats = [
  { value: '1', label: 'unified /query surface' },
  { value: '7', label: 'core filter operators' },
  { value: 'INF', label: 'room for expansion' },
]

export default function AuthLayout({
  badge,
  title,
  description,
  formTitle,
  formDescription,
  form,
  footer,
}: AuthLayoutProps) {
  const rootRef = useElegantAnimations<HTMLDivElement>([])

  return (
    <main
      ref={rootRef}
      className="page-shell flex min-h-[calc(100vh-116px)] items-center py-6 sm:py-8"
      data-animate="scene"
    >
      <div className="grid w-full gap-6 xl:grid-cols-[1.2fr_0.8fr]">
        <section className="surface-panel px-6 py-7 sm:px-8 sm:py-9 lg:px-10">
          <div
            className="absolute -left-12 top-20 h-36 w-36 rounded-full bg-[radial-gradient(circle,_rgba(255,166,111,0.72),_transparent_70%)] blur-3xl"
            data-float="slow"
            data-glow="pulse"
          />
          <div
            className="absolute right-8 top-14 h-32 w-32 rounded-full bg-[radial-gradient(circle,_rgba(82,215,255,0.62),_transparent_70%)] blur-3xl"
            data-float="medium"
          />

          <div className="relative z-10">
            <span className="section-badge" data-animate="chip">
              {badge}
            </span>

            <div className="mt-7 max-w-3xl">
              <p
                className="display-title text-[3rem] sm:text-[4rem] lg:text-[5rem]"
                data-animate="hero"
              >
                {title}
              </p>
              <p className="display-copy mt-5 max-w-2xl text-sm sm:text-base" data-animate="hero">
                {description}
              </p>
            </div>

            <div className="mt-7 flex flex-wrap gap-3">
              {showcasePoints.map((point) => (
                <span key={point} className="small-chip" data-animate="chip">
                  <span className="h-1.5 w-1.5 rounded-full bg-cyan-300" />
                  {point}
                </span>
              ))}
            </div>

            <div className="mt-8 grid gap-5 lg:grid-cols-[1.25fr_0.75fr]">
              <div className="surface-card p-5 sm:p-6" data-animate="panel" data-tilt>
                <div className="flex flex-wrap gap-2">
                  <span className="small-chip">Secure</span>
                  <span className="small-chip">Observable</span>
                  <span className="small-chip">Composable</span>
                </div>

                <div className="relative mt-5 overflow-hidden rounded-[26px] border border-cyan-400/20 bg-[#070a10] p-5 shadow-[0_0_45px_rgba(82,215,255,0.16)]">
                  <div className="absolute -left-4 top-8 h-28 w-28 rounded-full bg-[radial-gradient(circle,_rgba(255,166,111,0.48),_transparent_72%)] blur-2xl" />
                  <div className="absolute -right-6 bottom-2 h-36 w-36 rounded-full bg-[radial-gradient(circle,_rgba(82,215,255,0.44),_transparent_72%)] blur-2xl" />

                  <div className="relative z-10">
                    <p className="text-sm text-zinc-400">Describe the workflow you want to unlock</p>
                    <div className="mt-8 rounded-[22px] border border-white/8 bg-white/[0.02] p-4">
                      <p className="text-base text-zinc-100">
                        "Create a query-ready layer for the product database, preserve nested relations, and keep the client surface clean."
                      </p>

                      <div className="mt-10 flex flex-wrap items-center justify-between gap-3">
                        <div className="flex flex-wrap gap-2">
                          <span className="small-chip">Think deeper</span>
                          <span className="small-chip">Schema first</span>
                        </div>

                        <button
                          type="button"
                          className="primary-button px-4 py-2"
                          data-pressable
                        >
                          Launch
                        </button>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

                <div className="grid gap-4">
                {stats.map((stat) => {
                  const isNumeric = /^\d+$/.test(stat.value)

                  return (
                    <div key={stat.label} className="surface-card p-5" data-animate="panel" data-tilt>
                      <p
                        className="stat-value text-4xl text-white"
                        {...(isNumeric ? { 'data-count-up': stat.value } : {})}
                      >
                        {stat.value}
                      </p>
                      <p className="mt-2 text-sm text-zinc-400">{stat.label}</p>
                    </div>
                  )
                })}
              </div>
            </div>

            <div className="mt-8 grid gap-4 md:grid-cols-3">
              {featureCards.map((card) => (
                <article
                  key={card.title}
                  className="surface-card p-5"
                  data-animate="panel"
                  data-pressable
                  data-tilt
                >
                  <div className="mb-4 inline-flex rounded-full border border-white/8 bg-white/5 px-3 py-1 text-[11px] uppercase tracking-[0.24em] text-cyan-200">
                    Preview
                  </div>
                  <h2 className="text-lg font-semibold text-white">{card.title}</h2>
                  <p className="mt-3 text-sm leading-6 text-zinc-400">{card.copy}</p>
                </article>
              ))}
            </div>
          </div>
        </section>

        <section className="surface-panel px-6 py-7 sm:px-8 sm:py-9" data-animate="panel">
          <div className="relative z-10 flex h-full flex-col">
            <span className="section-badge w-fit" data-animate="chip">
              Workspace Access
            </span>
            <h1 className="display-title mt-6 text-[2.4rem] text-white sm:text-[3rem]">
              {formTitle}
            </h1>
            <p className="display-copy mt-4 text-sm sm:text-base">{formDescription}</p>

            <div className="mt-8 flex-1">{form}</div>
            <div className="subtle-divider mt-8" data-animate="line" />
            <div className="mt-5 text-sm text-zinc-400">{footer}</div>
          </div>
        </section>
      </div>
    </main>
  )
}
