# MC Mod Configuration System

The MC Mod - Auto-Fulfill Builder Requests now includes an auto-generated configuration file system that creates a `mc_mod.properties` file in the `config` folder.

## Overview

The configuration system provides:

1. **Auto-Generation**: On first run, the mod automatically creates `config/mc_mod.properties`
2. **Hot-Reloading**: Configuration changes take effect immediately
3. **Validation**: Invalid values are automatically corrected
4. **Logging**: Configuration changes are logged for transparency

## Configuration File

### Location
```
config/mc_mod.properties
```

### Default Configuration
```properties
# MC Mod - Auto-Fulfill Builder Requests Configuration
# Generated automatically - edit at your own risk

# Enable auto-fulfill builder requests
enable_auto_fulfill_builder_requests=true

# Log configuration changes
log_config_changes=true
```

## Configuration Options

### enable_auto_fulfill_builder_requests
- **Type**: Boolean
- **Default**: true (enabled by default)
- **Description**: Master toggle for the auto-fulfill feature
- **Values**: true/false

### log_config_changes
- **Type**: Boolean
- **Default**: true
- **Description**: Log when configuration values are changed
- **Values**: true/false

## Usage

### In-Game Commands
```
/mc_mod auto_fulfill status    # Check current configuration
/mc_mod auto_fulfill enable    # Enable auto-fulfill
/mc_mod auto_fulfill disable   # Disable auto-fulfill
```

### Manual Configuration
1. Stop the server/client
2. Edit `config/mc_mod.properties`
3. Restart the server/client

### Hot-Reloading
Configuration changes in the properties file take effect immediately without requiring a restart.

## Troubleshooting

### Configuration File Not Created
1. Ensure the mod is properly installed
2. Check that the mod loads without errors
3. Look for error messages in the server logs

### Invalid Configuration Values
The mod automatically corrects invalid values and logs the corrections:
```
[MC Mod] Corrected invalid configuration value: enable_auto_fulfill_builder_requests=false -> true
```

### Configuration Not Taking Effect
1. Check the server logs for configuration loading messages
2. Ensure the properties file is properly formatted
3. Verify file permissions allow the mod to read the file

## Development

### Adding New Configuration Options
1. Add the option to the `Config` class
2. Update the default configuration template
3. Add validation logic if needed
4. Update this documentation

### Configuration Validation
The mod includes built-in validation for all configuration options:
- Boolean values are automatically corrected
- Invalid values are logged and corrected
- Missing values are set to defaults

## File Structure
```
config/
└── mc_mod.properties    # Main configuration file
```

## Logging

Configuration-related log messages include:
- Configuration file creation
- Configuration value changes
- Invalid value corrections
- Configuration loading status

Example log messages:
```
[MC Mod] Configuration file created: config/mc_mod.properties
[MC Mod] Configuration loaded successfully
[MC Mod] Auto-fulfill builder requests: ENABLED
```

## Support

For configuration issues:
1. Check the server logs for error messages
2. Verify the properties file format
3. Try deleting the config file to regenerate it
4. Contact the mod author if problems persist 