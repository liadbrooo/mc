# 🚀 UltraOptimizer - Modern Minecraft Server Optimizer

<div align="center">

![Version](https://img.shields.io/badge/version-1.0.0-gold)
![Spigot](https://img.shields.io/badge/spigot-1.20-orange)
![License](https://img.shields.io/badge/license-MIT-green)

**The ultimate high-performance server optimization plugin with modern features**

</div>

---

## ✨ Features

### 🔧 Core Optimization
- **Entity Optimization** - Automatic cleanup of excess entities with smart thresholds
- **Chunk Management** - Aggressive unloading and spawn preloading
- **Tile Entity Optimization** - Hopper, furnace, and brewing stand optimizations
- **Mob AI Optimization** - Distance-based AI reduction for better performance
- **Redstone Optimization** - Clock speed limiting and lag machine prevention
- **Network Optimization** - Packet throttling and entity tracking improvements
- **Memory Management** - Automatic GC hints and cache management

### 📊 Performance Monitoring
- Real-time TPS monitoring with alerts
- Memory usage tracking
- Entity count monitoring by type
- Chunk load statistics
- Performance history logging

### 🗄️ Database Integration
- SQLite support (default)
- MySQL support (optional)
- Statistics logging
- Optimization history tracking

### 🎮 Modern Features
- MiniMessage support for rich text formatting
- Adventure API integration
- Action bar notifications
- Title notifications
- Sound alerts for critical issues

### 🔌 API & Extensions
- Public API for other plugins
- PlaceholderAPI support
- Vault integration
- ProtocolLib compatibility
- REST API (optional)

### ⚙️ Auto-Restart System
- Scheduled restarts with countdown
- RAM threshold-based restarts
- Configurable warning times

---

## 📥 Installation

1. Download the latest `.jar` file
2. Place it in your server's `plugins` folder
3. Restart your server
4. Configure `plugins/UltraOptimizer/config.yml` to your needs

---

## 💬 Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/uo status` | View server performance status | `ultraoptimizer.admin` |
| `/uo reload` | Reload configuration | `ultraoptimizer.admin` |
| `/uo cleanup [n]` | Remove n entities (default: 100) | `ultraoptimizer.admin` |
| `/uo stats` | View detailed statistics | `ultraoptimizer.admin` |
| `/uo version` | Show plugin version | `ultraoptimizer.use` |
| `/uo help` | Show help message | `ultraoptimizer.use` |

---

## 🔐 Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `ultraoptimizer.admin` | Full access to all commands | OP |
| `ultraoptimizer.use` | Use basic features | True |
| `ultraoptimizer.bypass` | Bypass optimizations | OP |
| `ultraoptimizer.notify` | Receive notifications | OP |

---

## ⚙️ Configuration

The config.yml is fully customizable. Key sections include:

```yaml
entity-optimization:
  auto-remove:
    enabled: true
    thresholds:
      mobs: 200
      items: 100
      arrows: 50
      
mob-ai:
  distance-based-ai:
    enabled: true
    full-ai-radius: 32
    reduced-ai-radius: 64
    no-ai-beyond: 96
    
monitoring:
  tps:
    alert-threshold: 18.0
    
auto-restart:
  enabled: false
  scheduled-restarts:
    - time: "04:00"
      delay-warning-minutes: 10
```

---

## 🔌 API Usage

```java
UltraOptimizerAPI api = UltraOptimizerMain.getInstance().getAPI();

// Get current TPS
double tps = api.getCurrentTPS();

// Force entity cleanup
int removed = api.forceGlobalEntityCleanup(100);

// Check if performance is optimal
boolean optimal = api.isPerformanceOptimal();

// Get performance summary
String summary = api.getPerformanceSummary();
```

---

## 📊 Metrics

UltraOptimizer uses bStats to collect anonymous metrics:
- Server count
- Player count
- Enabled features
- Performance improvements

You can disable metrics in the config:
```yaml
advanced:
  metrics:
    enabled: false
```

---

## 🛠️ Building from Source

```bash
git clone https://github.com/yourusername/UltraOptimizer.git
cd UltraOptimizer
mvn clean package
```

The compiled jar will be in the `target` folder.

---

## 📝 Requirements

- Java 17 or higher
- Spigot/Paper 1.20+ (compatible with older versions)
- Optional: PlaceholderAPI, Vault, ProtocolLib

---

## 🤝 Support

- Discord: [Join our server](#)
- Issues: [GitHub Issues](#)
- Wiki: [Documentation](#)

---

## 📜 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

<div align="center">

**Made with ❤️ for the Minecraft community**

[Download](#) • [Wiki](#) • [Discord](#) • [Report Bug](#)

</div>
