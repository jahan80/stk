import { useState, useRef } from "react"
import { useQuery } from "@tanstack/react-query"
import { apiClient } from "@/api/client"
import { Button } from "@/components/ui/button"
import { Loader2, Send } from "lucide-react"

interface Props {
  value: string
  onChange: (value: string) => void
  onSubmit: () => void
  isPending: boolean
  placeholder?: string
}

export function MentionInput({ value, onChange, onSubmit, isPending, placeholder }: Props) {
  const [showSuggestions, setShowSuggestions] = useState(false)
  const [filter, setFilter] = useState("")
  const [cursorPos, setCursorPos] = useState(0)
  const textareaRef = useRef<HTMLTextAreaElement>(null)

  const { data: usersData } = useQuery({
    queryKey: ["auth", "users"],
    queryFn: async () => {
      const { data } = await apiClient.get("/auth/users")
      return data
    },
  })

  const users = usersData?.data ?? []
  const filteredUsers = filter
    ? users.filter((u: any) => u.username.toLowerCase().includes(filter.toLowerCase()))
    : users

  const handleChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    const val = e.target.value
    onChange(val)

    const pos = e.target.selectionStart
    setCursorPos(pos)

    const textBefore = val.substring(0, pos)
    const atMatch = textBefore.match(/@([a-zA-Z0-9_.-]*)$/)

    if (atMatch) {
      setShowSuggestions(true)
      setFilter(atMatch[1])
    } else {
      setShowSuggestions(false)
      setFilter("")
    }
  }

  const insertMention = (username: string) => {
    const textBefore = value.substring(0, cursorPos)
    const textAfter = value.substring(cursorPos)
    const newBefore = textBefore.replace(/@([a-zA-Z0-9_.-]*)$/, `@${username} `)
    onChange(newBefore + textAfter)
    setShowSuggestions(false)
    setTimeout(() => textareaRef.current?.focus(), 0)
  }

  return (
    <div className="relative">
      <textarea
        ref={textareaRef}
        rows={3}
        placeholder={placeholder || "Write a reply... Type @ to mention someone"}
        value={value}
        onChange={handleChange}
        disabled={isPending}
        className="flex w-full rounded-md border border-input bg-background px-3 py-2 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
      />

      {showSuggestions && filteredUsers.length > 0 && (
        <div className="absolute bottom-full left-0 mb-1 max-h-48 w-72 overflow-y-auto rounded-md border bg-popover p-1 shadow-lg">
          <div className="px-2 py-1 text-xs text-muted-foreground">
            Mention user (typing @{filter || "..."})
          </div>
          {filteredUsers.slice(0, 8).map((u: any) => (
            <button
              key={u.id}
              type="button"
              onClick={() => insertMention(u.username)}
              className="flex w-full items-center gap-2 rounded-sm px-2 py-1 text-left text-sm hover:bg-accent"
            >
              <span className="font-mono text-xs text-muted-foreground">#{u.id}</span>
              <span className="font-medium">{u.username}</span>
              <span className="ml-auto text-xs text-muted-foreground">{u.role}</span>
            </button>
          ))}
        </div>
      )}

      <div className="mt-2 flex justify-end">
        <Button type="button" onClick={onSubmit} disabled={isPending || !value.trim()}>
          {isPending ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : <Send className="mr-2 h-4 w-4" />}
          Send
        </Button>
      </div>
    </div>
  )
}
