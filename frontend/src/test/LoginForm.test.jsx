import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import Login from '../pages/Login';
import * as AuthContext from '../context/AuthContext';

describe('Login Component', () => {
  it('renders inputs and submit button', () => {
    vi.spyOn(AuthContext, 'useAuth').mockReturnValue({
      login: vi.fn(),
      isAuthenticated: false
    });

    render(
      <MemoryRouter>
        <Login />
      </MemoryRouter>
    );

    expect(screen.getByLabelText(/email address/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/^password$/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /log in/i })).toBeInTheDocument();
  });

  it('displays error message when login fails', async () => {
    const mockLogin = vi.fn().mockRejectedValue({
      response: {
        data: {
          message: 'Invalid email or password'
        }
      }
    });

    vi.spyOn(AuthContext, 'useAuth').mockReturnValue({
      login: mockLogin,
      isAuthenticated: false
    });

    render(
      <MemoryRouter>
        <Login />
      </MemoryRouter>
    );

    fireEvent.change(screen.getByLabelText(/email address/i), {
      target: { value: 'user@example.com' }
    });
    fireEvent.change(screen.getByLabelText(/^password$/i), {
      target: { value: 'Password123' }
    });

    fireEvent.click(screen.getByRole('button', { name: /log in/i }));

    await waitFor(() => {
      expect(screen.getByText(/invalid email or password/i)).toBeInTheDocument();
    });
  });
}); 