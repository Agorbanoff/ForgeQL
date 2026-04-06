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
        const sceneTargets = utils.$('[data-animate="scene"]')
        const heroTargets = utils.$('[data-animate="hero"]')
        const panelTargets = utils.$('[data-animate="panel"]')
        const chipTargets = utils.$('[data-animate="chip"]')
        const lineTargets = utils.$('[data-animate="line"]')
        const slowFloatTargets = utils.$('[data-float="slow"]')
        const mediumFloatTargets = utils.$('[data-float="medium"]')
        const pulseTargets = utils.$('[data-glow="pulse"]')
        const countTargets = utils.$('[data-count-up]') as HTMLElement[]

        if (sceneTargets.length) {
          animate(sceneTargets, {
            opacity: [0, 1],
            filter: ['blur(14px)', 'blur(0px)'],
            translateY: [26, 0],
            scale: [0.992, 1],
            duration: 980,
            delay: stagger(70),
          })
        }

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

        if (lineTargets.length) {
          animate(lineTargets, {
            opacity: [0, 1],
            scaleX: [0, 1],
            duration: 840,
            delay: stagger(90),
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

        countTargets.forEach((element, index) => {
          const rawTarget = element.dataset.countUp
          if (!rawTarget) {
            return
          }

          const target = Number(rawTarget)
          if (Number.isNaN(target)) {
            return
          }

          const prefix = element.dataset.countPrefix ?? ''
          const suffix = element.dataset.countSuffix ?? ''
          const state = { value: 0 }

          animate(state, {
            value: target,
            duration: 1320,
            delay: index * 90,
            ease: 'outExpo',
            onUpdate: () => {
              element.textContent = `${prefix}${Math.round(state.value)}${suffix}`
            },
          })
        })
      }

      const tiltTargets = utils.$('[data-tilt]') as HTMLElement[]
      const pressables = utils.$('[data-pressable]:not([data-tilt])') as HTMLElement[]
      const cleanups: Array<() => void> = []

      tiltTargets.forEach((element) => {
        const resetTilt = () => {
          animate(element, {
            rotateX: 0,
            rotateY: 0,
            translateX: 0,
            translateY: 0,
            scale: 1,
            duration: 560,
            ease: 'out(4)',
          })
        }

        const handleMove = (event: PointerEvent) => {
          if (reduceMotion) {
            return
          }

          const rect = element.getBoundingClientRect()
          if (!rect.width || !rect.height) {
            return
          }

          const ratioX = (event.clientX - rect.left) / rect.width
          const ratioY = (event.clientY - rect.top) / rect.height

          element.style.setProperty('--spot-x', `${ratioX * 100}%`)
          element.style.setProperty('--spot-y', `${ratioY * 100}%`)

          animate(element, {
            rotateY: (ratioX - 0.5) * 8,
            rotateX: (0.5 - ratioY) * 7,
            translateX: (ratioX - 0.5) * 4,
            translateY: (ratioY - 0.5) * 4,
            scale: 1.01,
            duration: 320,
            ease: 'out(3)',
          })
        }

        const handleLeave = () => {
          if (reduceMotion) {
            return
          }

          element.style.removeProperty('--spot-x')
          element.style.removeProperty('--spot-y')
          resetTilt()
        }

        element.addEventListener('pointermove', handleMove)
        element.addEventListener('pointerleave', handleLeave)

        cleanups.push(() => {
          element.removeEventListener('pointermove', handleMove)
          element.removeEventListener('pointerleave', handleLeave)
        })
      })

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
            translateY: -3,
            scale: 1.018,
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
            scale: [1, 0.964, 1.01, 1],
            rotate: [0, -0.16, 0.08, 0],
            duration: 560,
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
