# Contributing to QR2PC

Thank you for your interest in contributing to QR2PC. To make collaboration smooth, please follow the guidelines below.

## How to Contribute

1. Fork the repository.
2. Create a new branch for your work:

```bash
git checkout -b feature/your-feature-name
```

3. Make your changes in a clean, focused manner.
4. Test locally and verify the application still runs correctly.
5. Open a pull request describing the change clearly.

## Reporting Issues

If you find a bug or want to request a feature, create an issue with:

- a descriptive title
- expected behavior
- actual behavior
- steps to reproduce
- environment details (Python version, OS)

## Coding Style

- Keep Python code readable and organized.
- Use descriptive variable and function names.
- Add comments only when a behavior is not obvious.
- Maintain consistent formatting for strings and logging.

## Testing Your Changes

- Run the application after any code changes.
- Verify Firebase connection and scan processing behavior if relevant.
- Ensure `scanner_listener.py` starts without errors.

## Pull Request Checklist

- [ ] Code is well-structured and easy to understand
- [ ] No sensitive data or credentials are included
- [ ] Changes are limited to the intended scope
- [ ] Documentation is updated as needed

## Notes

- `serviceAccountKey.json` must not be committed.
- Build artifacts and output directories are ignored by `.gitignore`.
- Keep the UI workflow clear and the Firebase path configuration accurate.
