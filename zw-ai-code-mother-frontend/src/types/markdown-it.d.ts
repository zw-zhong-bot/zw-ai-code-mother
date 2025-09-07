declare module 'markdown-it' {
  interface MarkdownItOptions {
    html?: boolean
    linkify?: boolean
    typographer?: boolean
    highlight?: (str: string, lang: string) => string
  }

  class MarkdownIt {
    constructor(options?: MarkdownItOptions)
    render(text: string): string
  }

  export = MarkdownIt
}
