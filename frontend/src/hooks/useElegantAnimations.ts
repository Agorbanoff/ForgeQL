import { useRef, type DependencyList } from 'react'

export function useElegantAnimations<T extends HTMLElement>(
  _dependencies: DependencyList = []
) {
  return useRef<T | null>(null)
}
