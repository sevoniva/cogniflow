declare global {
  var mockFetch: (data: unknown) => void
  var mockFetchError: (status?: number, statusText?: string) => void
}

export {}
