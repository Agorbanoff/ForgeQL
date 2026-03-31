import { animate, createScope, stagger, utils } from 'animejs'
import { useEffect, useRef, type DependencyList } from 'react'

export function useElegantAnimations<T extends HTMLElement>(
  dependencies: DependencyList = []
) {
  const rootRef = useRef<T | null>(null)

  useEffect(() => {
    if (!rootRef.current) {
      return
    }

    const scope = createScope({
      root: rootRef.current,
      defaults: {
        duration: 720,
        ease: 'out(4)',
      },
      mediaQueries: {
        reducedMotion: '(prefers-reduced-motion: reduce)',
      },
    }).add((self) => {
      if (!self) {
        return
      }

      const reduceMotion = self.matches.reducedMotion

      if (!reduceMotion) {
        const heroTargets = utils.$('[data-animate="hero"]')
        const panelTargets = utils.$('[data-animate="panel"]')
        const chipTargets = utils.$('[data-animate="chip"]')
        const slowFloatTargets = utils.$('[data-float="slow"]')
        const mediumFloatTargets = utils.$('[data-float="medium"]')
        const pulseTargets = utils.$('[data-glow="pulse"]')

        if (heroTargets.length) {
          animate(heroTargets, {
            opacity: [0, 1],
            translateY: [32, 0],
            scale: [0.97, 1],
            duration: 1100,
            delay: stagger(130),
          })
        }

        if (panelTargets.length) {
          animate(panelTargets, {
            opacity: [0, 1],
            translateY: [28, 0],
            scale: [0.985, 1],
            duration: 900,
            delay: stagger(90),
          })
        }

        if (chipTargets.length) {
          animate(chipTargets, {
            opacity: [0, 1],
            translateY: [14, 0],
            duration: 640,
            delay: stagger(45),
          })
        }

        if (slowFloatTargets.length) {
          animate(slowFloatTargets, {
            translateY: ['-0.45rem', '0.45rem'],
            duration: 4200,
            alternate: true,
            loop: true,
            ease: 'inOutSine',
            delay: stagger(220),
          })
        }

        if (mediumFloatTargets.length) {
          animate(mediumFloatTargets, {
            translateY: ['0.3rem', '-0.3rem'],
            duration: 3400,
            alternate: true,
            loop: true,
            ease: 'inOutSine',
            delay: stagger(180),
          })
        }

        if (pulseTargets.length) {
          animate(pulseTargets, {
            opacity: [0.42, 0.88],
            scale: [0.98, 1.04],
            duration: 2800,
            alternate: true,
            loop: true,
            ease: 'inOutSine',
            delay: stagger(120),
          })
        }
      }

      const pressables = utils.$('[data-pressable]') as HTMLElement[]
      const cleanups: Array<() => void> = []

      pressables.forEach((element) => {
        const reset = () =>
          animate(element, {
            translateY: 0,
            scale: 1,
            duration: 420,
          })

        const handleEnter = () => {
          if (reduceMotion || (element instanceof HTMLButtonElement && element.disabled)) {
            return
          }

          animate(element, {
            translateY: -2,
            scale: 1.01,
            duration: 320,
          })
        }

        const handleLeave = () => {
          if (reduceMotion) {
            return
          }

          reset()
        }

        const handlePress = () => {
          if (reduceMotion || (element instanceof HTMLButtonElement && element.disabled)) {
            return
          }

          animate(element, {
            scale: [1, 0.972, 1],
            rotate: [0, -0.1, 0],
            duration: 520,
            ease: 'out(5)',
          })
        }

        const handleFocus = () => {
          if (reduceMotion) {
            return
          }

          animate(element, {
            scale: 1.01,
            duration: 260,
          })
        }

        const handleBlur = () => {
          if (reduceMotion) {
            return
          }

          reset()
        }

        element.addEventListener('pointerenter', handleEnter)
        element.addEventListener('pointerleave', handleLeave)
        element.addEventListener('pointerdown', handlePress)
        element.addEventListener('focus', handleFocus)
        element.addEventListener('blur', handleBlur)

        cleanups.push(() => {
          element.removeEventListener('pointerenter', handleEnter)
          element.removeEventListener('pointerleave', handleLeave)
          element.removeEventListener('pointerdown', handlePress)
          element.removeEventListener('focus', handleFocus)
          element.removeEventListener('blur', handleBlur)
        })
      })

      return () => {
        cleanups.forEach((cleanup) => cleanup())
      }
    })

    return () => {
      scope.revert()
    }
  }, dependencies)

  return rootRef
}
