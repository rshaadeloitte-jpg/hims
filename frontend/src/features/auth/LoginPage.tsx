import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useLogin, useGenerateOtp, useVerifyOtp } from './queries';
import { loginSchema, otpVerifySchema } from './zodSchema';
import type { LoginValues, OtpVerifyValues } from './zodSchema';

type AuthMode = 'credentials' | 'otp';

export function LoginPage() {
  const [mode, setMode] = useState<AuthMode>('credentials');
  const [otpMobile, setOtpMobile] = useState('');
  const login = useLogin();
  const generateOtp = useGenerateOtp();
  const verifyOtp = useVerifyOtp();

  const { register, handleSubmit, formState: { errors } } = useForm<LoginValues>({
    resolver: zodResolver(loginSchema),
  });

  const { register: regOtp, handleSubmit: submitOtp, watch: watchOtp, formState: { errors: otpErrors } } = useForm<OtpVerifyValues>({
    resolver: zodResolver(otpVerifySchema),
  });

  const otpCode = watchOtp('otp');

  const onCredentialLogin = async (data: LoginValues) => {
    try {
      const tokens = await login.mutateAsync(data);
      localStorage.setItem('access_token', tokens.accessToken);
      window.location.href = '/';
    } catch (e) { /* error displayed below */ }
  };

  const onSendOtp = async (mobileNo: string) => {
    setOtpMobile(mobileNo);
    await generateOtp.mutateAsync({ mobileNo });
  };

  const onVerifyOtpSubmit = async (data: OtpVerifyValues) => {
    try {
      const tokens = await verifyOtp.mutateAsync(data);
      localStorage.setItem('access_token', tokens.accessToken);
      window.location.href = '/';
    } catch (e) { /* error displayed below */ }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50">
      <div className="w-full max-w-md bg-white rounded-lg shadow-sm border border-gray-200 p-8">
        <div className="mb-8">
          <h1 className="text-2xl font-semibold text-gray-900">HIMS</h1>
          <p className="text-sm text-gray-500 mt-1">Hospital Information Management System</p>
        </div>

        <div className="flex rounded-lg bg-gray-100 p-1 mb-6" role="group" aria-label="Login method">
          <button
            className={`flex-1 py-2 text-sm font-medium rounded-md transition-colors ${
              mode === 'credentials' ? 'bg-white text-gray-900 shadow-sm' : 'text-gray-500 hover:text-gray-700'
            }`}
            onClick={() => setMode('credentials')}
          >
            Username & Password
          </button>
          <button
            className={`flex-1 py-2 text-sm font-medium rounded-md transition-colors ${
              mode === 'otp' ? 'bg-white text-gray-900 shadow-sm' : 'text-gray-500 hover:text-gray-700'
            }`}
            onClick={() => setMode('otp')}
          >
            OTP Login
          </button>
        </div>

        {mode === 'credentials' ? (
          <form onSubmit={handleSubmit(onCredentialLogin)} className="space-y-4">
            <div>
              <label htmlFor="username" className="block text-sm font-medium text-gray-700">Username</label>
              <input
                id="username" type="text" {...register('username')}
                className="mt-1 w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:ring-1 focus:ring-gray-900 focus:border-gray-900"
                autoComplete="username"
              />
              {errors.username && <p className="mt-1 text-sm text-red-600">{errors.username.message}</p>}
            </div>
            <div>
              <label htmlFor="password" className="block text-sm font-medium text-gray-700">Password</label>
              <input
                id="password" type="password" {...register('password')}
                className="mt-1 w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:ring-1 focus:ring-gray-900 focus:border-gray-900"
                autoComplete="current-password"
              />
              {errors.password && <p className="mt-1 text-sm text-red-600">{errors.password.message}</p>}
            </div>
            <button
              type="submit" disabled={login.isPending}
              className="w-full py-2 bg-gray-900 text-white text-sm font-medium rounded-md hover:bg-gray-800 disabled:opacity-50"
            >
              {login.isPending ? 'Signing in...' : 'Sign In'}
            </button>
            {login.isError && <div className="text-sm text-red-600 text-center">Login failed. Check credentials.</div>}
          </form>
        ) : (
          <div className="space-y-4">
            {!otpMobile ? (
              <form onSubmit={handleSubmit((d) => onSendOtp(d.username))} className="space-y-4">
                <div>
                  <label htmlFor="mobile-otp" className="block text-sm font-medium text-gray-700">Mobile Number</label>
                  <input
                    id="mobile-otp" type="tel" placeholder="9876543210"
                    className="mt-1 w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:ring-1 focus:ring-gray-900 focus:border-gray-900"
                  />
                </div>
                <button type="submit" className="w-full py-2 bg-gray-900 text-white text-sm font-medium rounded-md hover:bg-gray-800">
                  Send OTP
                </button>
              </form>
            ) : (
              <form onSubmit={submitOtp(onVerifyOtpSubmit)} className="space-y-4">
                <p className="text-sm text-gray-600">OTP sent to <strong>{otpMobile}</strong></p>
                <div>
                  <label htmlFor="otp" className="block text-sm font-medium text-gray-700">Enter OTP</label>
                  <input
                    id="otp" type="text" inputMode="numeric" maxLength={6} {...regOtp('otp')}
                    className="mt-1 w-full px-3 py-2 border border-gray-300 rounded-md text-sm font-mono tracking-[0.5em] text-center"
                    placeholder="000000"
                  />
                  {otpErrors.otp && <p className="mt-1 text-sm text-red-600">{otpErrors.otp.message}</p>}
                </div>
                <button
                  type="submit" disabled={verifyOtp.isPending || (otpCode?.length ?? 0) !== 6}
                  className="w-full py-2 bg-gray-900 text-white text-sm font-medium rounded-md hover:bg-gray-800 disabled:opacity-50"
                >
                  {verifyOtp.isPending ? 'Verifying...' : 'Verify & Sign In'}
                </button>
                {verifyOtp.isError && <div className="text-sm text-red-600 text-center">Invalid OTP. Try again.</div>}
              </form>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
