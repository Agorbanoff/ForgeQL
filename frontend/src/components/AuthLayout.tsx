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
  highlights: string[]
}

export default function AuthLayout({
  badge,
  title,
  description,
  formTitle,
  formDescription,
  form,
  footer,
  highlights,
}: AuthLayoutProps) {
  const rootRef = useElegantAnimations<HTMLDivElement>([])

  return (
    <main
      ref={rootRef}
      className="page-shell flex h-[100dvh] items-center justify-center overflow-hidden py-3 sm:py-4"
      data-animate="scene"
    >
      <div className="mx-auto grid w-full max-w-[min(92vw,1160px)] items-center gap-4 lg:grid-cols-[minmax(19rem,0.94fr)_minmax(21rem,1.06fr)]">
        <section className="surface-panel px-5 py-5 sm:px-6 sm:py-6 lg:px-7">
          <div
            className="absolute -left-12 top-16 h-36 w-36 rounded-full bg-[radial-gradient(circle,_rgba(255,166,111,0.58),_transparent_70%)] blur-3xl"
            data-float="slow"
            data-glow="pulse"
          />
          <div
            className="absolute right-8 top-14 h-32 w-32 rounded-full bg-[radial-gradient(circle,_rgba(82,215,255,0.54),_transparent_70%)] blur-3xl"
            data-float="medium"
          />

          <div className="relative z-10 flex h-full flex-col justify-center">
            <p className="text-center text-xs uppercase tracking-[0.32em] text-zinc-500 lg:text-left">
              ForgeQL
            </p>
            <span className="section-badge mt-4 w-fit self-center lg:self-start" data-animate="chip">
              {badge}
            </span>

            <div className="mt-4 max-w-3xl text-center lg:text-left">
              <p
                className="display-title mx-auto max-w-[11ch] text-[2.5rem] text-white sm:text-[3.15rem] lg:mx-0 lg:text-[3.7rem]"
                data-animate="hero"
              >
                {title}
              </p>
              <p
                className="display-copy mt-3 max-w-2xl text-sm leading-6 sm:text-[0.92rem]"
                data-animate="hero"
              >
                {description}
              </p>
            </div>

            <div className="mt-5 grid gap-2.5">
              {highlights.map((highlight, index) => (
                <article
                  key={highlight}
                  className="surface-card flex items-start gap-3 p-3 sm:p-3.5"
                  data-animate="panel"
                  data-tilt
                >
                  <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-2xl border border-cyan-300/18 bg-cyan-300/10 text-sm font-semibold text-cyan-100">
                    0{index + 1}
                  </span>
                  <p className="text-sm leading-6 text-zinc-300">{highlight}</p>
                </article>
              ))}
            </div>
          </div>
        </section>

        <section className="surface-panel px-5 py-5 sm:px-6 sm:py-6" data-animate="panel">
          <div className="relative z-10 flex h-full flex-col justify-center">
            <span className="section-badge w-fit self-center lg:self-start" data-animate="chip">
              Workspace access
            </span>
            <h1 className="display-title mt-4 text-center text-[2rem] text-white sm:text-[2.55rem] lg:text-left">
              {formTitle}
            </h1>
            <p className="display-copy mt-2.5 text-center text-sm leading-6 sm:text-[0.92rem] lg:text-left">
              {formDescription}
            </p>

            <div className="mt-6 flex-1">{form}</div>
            <div className="subtle-divider mt-5" data-animate="line" />
            <div className="mt-3 text-center text-sm text-zinc-400 lg:text-left">{footer}</div>
          </div>
        </section>
      </div>
    </main>
  )
}
