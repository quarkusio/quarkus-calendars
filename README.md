# Quarkus Calendars

A Quarkus CLI application for managing community-driven calendar events on Google Calendar.

## Overview

This application enables community members to create and manage calendar events through pull requests. Events are
defined in YAML files and automatically published to Google Calendar.

## Calendars

The project manages two public Google Calendars that you can subscribe to:

### Quarkus Releases

Tracks release dates for Quarkus core and platform.

- **View online**: [Quarkus Releases Calendar](https://calendar.google.com/calendar/embed?src=96e7edd0aa40dfc869532c230d06bf6502524e5ad4c9e6d1ef775923937451b5%40group.calendar.google.com&ctz=Europe%2FParis)
- **Subscribe (iCal)**: `https://calendar.google.com/calendar/ical/96e7edd0aa40dfc869532c230d06bf6502524e5ad4c9e6d1ef775923937451b5%40group.calendar.google.com/public/basic.ics`

### Quarkus Community Calls

Manages community call schedules with video call links.

- **View online**: [Quarkus Calls Calendar](https://calendar.google.com/calendar/embed?src=935090aaae38be35eda437d018782b7827f3770f7a0344013677acd861570b20%40group.calendar.google.com&ctz=Europe%2FParis)
- **Subscribe (iCal)**: `https://calendar.google.com/calendar/ical/935090aaae38be35eda437d018782b7827f3770f7a0344013677acd861570b20%40group.calendar.google.com/public/basic.ics`

## Repository Structure

Events are described using YAML files, with each event stored in a separate file:

- `quarkus-releases/`: Contains release event YAML files
- `quarkus-calls/`: Contains community call event YAML files

## Submit a New Release Event

Release events are all-day events that mark Quarkus core or platform releases.

### Directory

Place your YAML file in the `quarkus-releases/` directory at the repository root.

### YAML Structure

```yaml
type: release
title: Quarkus 3.17.0 Release
date: 2025-11-15
```

**Fields:**
- `type`: Must be `release`
- `title`: (Required) Name of the release
- `date`: (Required) Release date in `YYYY-MM-DD` format

**Constraints:**
- Release events are always all-day events
- Do not include: `description`, `time`, `duration`, or `callLink`

### Example

File: `quarkus-releases/quarkus-3.17.0.yaml`

```yaml
type: release
title: Quarkus 3.17.0 Release
date: 2025-11-15
```

## Submit a New Call Event

Call events are scheduled community calls with a specific time and video call link.

### Directory

Place your YAML file in the `quarkus-calls/` directory at the repository root.

### YAML Structure

```yaml
type: call
title: November 2025 Quarkus Community Call
description: Monthly community sync to discuss recent developments, upcoming features, and answer questions from the community.
date: 2025-11-18
time: 14:00:00
duration: PT50M
callLink: https://meet.google.com/abc-defg-hij
```

**Fields:**
- `type`: Must be `call`
- `title`: (Required) Name of the call
- `description`: (Required) Description of the call's purpose
- `date`: (Required) Call date in `YYYY-MM-DD` format
- `time`: (Required) Call time in UTC, format `HH:MM:SS`
- `duration`: (Optional) Duration in ISO-8601 format (default: `PT50M` = 50 minutes)
- `callLink`: (Required) Video call URL (Google Meet, Zoom, etc.)

**Constraints:**
- Call events cannot be all-day events
- All fields except `duration` are required
- Time must be in UTC

### Example

File: `quarkus-calls/november-2025-community-call.yaml`

```yaml
type: call
title: November 2025 Quarkus Community Call
description: Monthly community sync to discuss recent developments, upcoming features, and answer questions from the community.
date: 2025-11-18
time: 14:00:00
duration: PT50M
callLink: https://meet.google.com/abc-defg-hij
```

## Usage

This is a Quarkus CLI application using picocli for parameter handling.

```bash
# Build the application
./mvnw package

# Run the application
java -jar target/quarkus-app/quarkus-run.jar [options]
```

## Requirements

- Java 21
- Maven 3.9+
- Google Calendar API credentials
