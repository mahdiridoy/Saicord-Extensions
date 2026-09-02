# Saicord CloudStream Extensions

CloudStream extensions for watching Bengali and Hindi dubbed movies and series from [Saicord.com](https://saicord.com).

## Features

- **Bengali (bn)**: Watch Bengali dubbed movies, series, cartoons, and TV+ content
- **Hindi (hi)**: Watch Hindi dubbed movies, series, cartoons, and TV+ content
- **Search**: Search for movies and series by title
- **Browse**: Browse content by categories (Latest, Trending, Top Rated, Coming Soon)
- **Details**: View movie/series details including poster, description, rating, and more
- **Recommendations**: Get recommendations for similar content

## Installation

### Method 1: Add Repository (Recommended)

1. Open CloudStream app
2. Go to **Settings** > **Extensions**
3. Tap **Add Repository** (or **+** button)
4. Enter the repository URL:
   ```
   https://raw.githubusercontent.com/mahdiridoy/Saicord-Extensions/master/repo.json
   ```
5. Tap **OK** to save
6. Go back to **Extensions** and find **Saicord (Bengali)** and **Saicord (Hindi)**
7. Tap **Install** on each extension

### Method 2: Manual Installation

1. Download the latest `.cs3` files from [Releases](https://github.com/mahdiridoy/Saicord-Extensions/releases)
2. Open CloudStream app
3. Go to **Settings** > **Extensions**
4. Tap **Install from file**
5. Select the downloaded `.cs3` file

## Supported Content Types

| Type | Bengali | Hindi |
|------|---------|-------|
| Movies | Yes | Yes |
| Series | Yes | Yes |
| Cartoons | Yes | Yes |
| TV+ | Yes | Yes |

## Building from Source

### Prerequisites

- Android Studio (latest version)
- JDK 17
- Android SDK

### Build Steps

1. Clone the repository:
   ```bash
   git clone https://github.com/mahdiridoy/Saicord-Extensions.git
   cd Saicord-Extensions
   ```

2. Build using Gradle:
   ```bash
   # Windows
   .\gradlew.bat SaicordProvider:make

   # Linux/Mac
   ./gradlew SaicordProvider:make
   ```

3. The built `.cs3` file will be in `SaicordProvider/build/outputs/`

### Deploy to Device

1. Connect your Android device via USB
2. Enable USB debugging
3. Run:
   ```bash
   # Windows
   .\gradlew.bat SaicordProvider:deployWithAdb

   # Linux/Mac
   ./gradlew SaicordProvider:deployWithAdb
   ```

## Project Structure

```
Saicord-Extensions/
├── build.gradle.kts              # Root build configuration
├── settings.gradle.kts           # Project settings
├── gradle.properties             # Gradle properties
├── repo.json                     # CloudStream repository JSON
├── SaicordProvider/
│   ├── build.gradle.kts          # Provider build configuration
│   └── src/main/
│       ├── AndroidManifest.xml   # Android manifest
│       └── kotlin/com/saicord/provider/
│           ├── SaicordBase.kt    # Base class with common functionality
│           ├── SaicordBn.kt      # Bengali provider
│           ├── SaicordHi.kt      # Hindi provider
│           └── SaicordPlugin.kt  # Plugin registration
└── .github/workflows/
    └── build.yml                 # GitHub Actions workflow
```

## How It Works

The extension scrapes the Saicord website to provide content in CloudStream:

1. **Main Page**: Fetches and displays content from various categories
2. **Search**: Uses the site's search functionality to find content
3. **Details**: Parses movie/series details including metadata
4. **Video Links**: Extracts video sources from the page (may use extractors for some hosts)

## Troubleshooting

### Cloudflare Issues

Saicord uses Cloudflare protection. If you encounter issues:

1. The extension includes headers to help bypass basic protection
2. Some content may require additional extractors
3. If videos don't play, try updating the extension

### No Content Loading

1. Check your internet connection
2. Try clearing CloudStream cache
3. Update to the latest version of the extension

### Videos Not Playing

1. Some videos may use external hosts that require specific extractors
2. Try using a different server if available
3. Report issues with specific content details

## Contributing

Contributions are welcome! Here's how to help:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Adding New Features

- **New Extractors**: Add support for new video hosts
- **Improved Parsing**: Enhance content extraction
- **UI Improvements**: Better error messages and loading states

## Legal Notice

This extension is for educational purposes only. It is not affiliated with or endorsed by Saicord. The extension scrapes publicly available content from the website. Users are responsible for complying with their local laws and regulations regarding content consumption.

## Support

If you encounter issues or have questions:

1. Check the [Issues](https://github.com/mahdiridoy/Saicord-Extensions/issues) page
2. Create a new issue with detailed description
3. Include device model, Android version, and CloudStream version

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- [CloudStream](https://github.com/recloudstream/cloudstream) - The amazing media streaming app
- [Saicord](https://saicord.com) - For providing Bengali and Hindi dubbed content
- All contributors and testers

---

**Disclaimer**: This extension is not affiliated with, maintained, authorized, sponsored, or officially connected with Saicord or any of its affiliates. This is an independent and unofficial extension.
