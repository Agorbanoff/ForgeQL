import { AnimatedSelect, type AnimatedSelectOption } from '../AnimatedSelect'

type Props = {
  value: string
  options: readonly AnimatedSelectOption[]
  onChange: (value: string) => void
  disabled?: boolean
  ariaLabel: string
}

export function Dropdown(props: Props) {
  return <AnimatedSelect {...props} />
}
