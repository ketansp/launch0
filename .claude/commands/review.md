Review the current uncommitted changes for code quality.

1. Run `git diff` to see all staged and unstaged changes
2. Check for:
   - Privacy violations (analytics, tracking, unwanted network calls)
   - Security issues (OWASP top 10)
   - Consistency with existing code patterns (View Binding, extension functions, Constants organization)
   - Missing landscape layout updates
   - Missing localization strings
   - ProGuard/R8 implications for new classes using reflection
3. Provide a summary with actionable feedback
