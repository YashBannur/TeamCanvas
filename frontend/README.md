# TeamCanvas Frontend

A collaborative project management web application for development teams featuring workspace management, member role assignments, invitation handling, project tracking, and an interactive drag-and-drop Kanban board.

## Technology Stack

- **Framework**: React 19
- **Bundler & Tooling**: Vite 8, Oxlint
- **Routing**: React Router v7 (`BrowserRouter`)
- **HTTP Client**: Axios with HttpOnly refresh cookie rotation
- **Drag & Drop**: `@dnd-kit/core`, `@dnd-kit/sortable`, `@dnd-kit/utilities`
- **Testing**: Vitest, React Testing Library, JSDOM

## Setup & Local Development

### 1. Environment Configuration

Create a `.env` file in the frontend root:

```env
VITE_API_URL=http://localhost:8080
# Optional: Set to true if you want to test the dashboard with local mock data before backend endpoint deployment
VITE_USE_MOCK_DASHBOARD=false