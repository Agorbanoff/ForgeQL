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
      className="page-shell flex min-h-screen items-center py-10 sm:py-14"
      data-animate="scene"
    >
      <div className="grid w-full max-w-5xl gap-6 lg:grid-cols-[0.92fr_1.08fr]">
        <section className="surface-panel px-6 py-8 sm:px-8 sm:py-9 lg:px-10">
          <div
            className="absolute -left-12 top-16 h-36 w-36 rounded-full bg-[radial-gradient(circle,_rgba(255,166,111,0.58),_transparent_70%)] blur-3xl"
            data-float="slow"
            data-glow="pulse"
          />
          <div
            className="absolute right-8 top-14 h-32 w-32 rounded-full bg-[radial-gradient(circle,_rgba(82,215,255,0.54),_transparent_70%)] blur-3xl"
            data-float="medium"
          />

          <div className="relative z-10 flex h-full flex-col">
            <p className="text-xs uppercase tracking-[0.32em] text-zinc-500">
              SigmaQL
            </p>
            <span className="section-badge mt-5 w-fit" data-animate="chip">
              {badge}
            </span>

            <div className="mt-7 max-w-3xl">
              <p
                className="display-title max-w-[11ch] text-[3rem] text-white sm:text-[4rem] lg:text-[4.6rem]"
                data-animate="hero"
              >
                {title}
              </p>
              <p
                className="display-copy mt-5 max-w-2xl text-sm sm:text-base"
                data-animate="hero"
              >
                {description}
              </p>
            </div>

            <div className="mt-8 grid gap-3">
              {highlights.map((highlight, index) => (
                <article
                  key={highlight}
                  className="surface-card flex items-start gap-4 p-4"
                  data-animate="panel"
                  data-tilt
                >
                  <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-2xl border border-cyan-300/18 bg-cyan-300/10 text-sm font-semibold text-cyan-100">
                    0{index + 1}
                  </span>
                  <p className="text-sm leading-6 text-zinc-300">{highlight}</p>
                </article>
              ))}
            </div>
          </div>
        </section>

        <section className="surface-panel px-6 py-8 sm:px-8 sm:py-9" data-animate="panel">
          <div className="relative z-10 flex h-full flex-col">
            <span className="section-badge w-fit" data-animate="chip">
              Workspace access
            </span>
            <h1 className="display-title mt-6 text-[2.4rem] text-white sm:text-[3rem]">
              {formTitle}
            </h1>
            <p className="display-copy mt-4 text-sm sm:text-base">
              {formDescription}
            </p>

            <div className="mt-8 flex-1">{form}</div>
            <div className="subtle-divider mt-8" data-animate="line" />
            <div className="mt-5 text-sm text-zinc-400">{footer}</div>
          </div>
        </section>
      </div>
    </main>
  )
}
