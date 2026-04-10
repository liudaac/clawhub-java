export function useErrorHandler() {
  const toast = inject('toast') as { 
    error: (msg: string, title?: string) => void 
    warning: (msg: string, title?: string) => void
  }

  function handleError(error: unknown, context?: string) {
    const message = error instanceof Error ? error.message : String(error)
    
    // Handle specific error types
    if (message.includes('Rate limit')) {
      toast.warning(message, 'Too Many Requests')
    } else if (message.includes('Network') || message.includes('fetch')) {
      toast.error('Network error. Please check your connection.', 'Connection Error')
    } else if (message.includes('401') || message.includes('Unauthorized')) {
      toast.error('Please log in to continue.', 'Authentication Required')
    } else if (message.includes('403') || message.includes('Forbidden')) {
      toast.error('You do not have permission to perform this action.', 'Access Denied')
    } else {
      toast.error(context ? `${context}: ${message}` : message, 'Error')
    }
    
    console.error(context || 'Error:', error)
  }

  function handleAsync<T>(
    promise: Promise<T>,
    context?: string
  ): Promise<T | undefined> {
    return promise.catch((error) => {
      handleError(error, context)
      return undefined
    })
  }

  return {
    handleError,
    handleAsync,
  }
}
