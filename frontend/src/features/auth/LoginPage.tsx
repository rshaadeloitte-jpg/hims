import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import {
  Box,
  Button,
  Card,
  Flex,
  Heading,
  HStack,
  Text,
  VStack,
} from '@chakra-ui/react';
import { DeloitteLogo } from '../../components/DeloitteIcon';
import { FormField, TextInput } from '../../components/ui/form';
import { useLogin, useGenerateOtp, useVerifyOtp } from './queries';
import { loginSchema, otpVerifySchema } from './zodSchema';
import type { LoginValues, OtpVerifyValues } from './zodSchema';

type AuthMode = 'credentials' | 'otp';


function persistToken(token: string) {
  try { localStorage.setItem('access_token', token); } catch { /* ignore */ }
  try { sessionStorage.setItem('access_token', token); } catch { /* ignore */ }
  try { document.cookie = `access_token=${encodeURIComponent(token)}; path=/; SameSite=Lax`; } catch { /* ignore */ }
}

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
      persistToken(tokens.accessToken);
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
      persistToken(tokens.accessToken);
      window.location.href = '/';
    } catch (e) { /* error displayed below */ }
  };

  return (
    <Flex minH="60vh" align="center" justify="center" py={10}>
      <Card.Root w="full" maxW="md" bg="white" boxShadow="md">
        <Card.Body p={8}>
          <HStack gap={3} mb={6}>
            <DeloitteLogo size={36} />
            <VStack gap={0} align="flex-start" lineHeight={1.2}>
              <Heading as="h1" size="lg" color="black" m={0}>
                HIMS
              </Heading>
              <Text fontSize="sm" color="gray.600" m={0}>
                Hospital Information Management System
              </Text>
            </VStack>
          </HStack>

          <HStack
            bg="gray.100"
            p={1}
            mb={6}
            borderRadius="lg"
            role="group"
            aria-label="Login method"
            gap={0}
          >
            <Button
              flex={1}
              size="sm"
              variant={mode === 'credentials' ? 'secondary' : 'ghost'}
              boxShadow={mode === 'credentials' ? 'sm' : undefined}
              bg={mode === 'credentials' ? 'white' : undefined}
              onClick={() => setMode('credentials')}
            >
              Username &amp; Password
            </Button>
            <Button
              flex={1}
              size="sm"
              variant={mode === 'otp' ? 'secondary' : 'ghost'}
              boxShadow={mode === 'otp' ? 'sm' : undefined}
              bg={mode === 'otp' ? 'white' : undefined}
              onClick={() => setMode('otp')}
            >
              OTP Login
            </Button>
          </HStack>

          {mode === 'credentials' ? (
            <VStack
              as="form"
              gap={4}
              align="stretch"
              onSubmit={handleSubmit(onCredentialLogin)}
            >
              <FormField label="Username" error={errors.username?.message} required>
                <TextInput
                  id="username"
                  type="text"
                  autoComplete="username"
                  {...register('username')}
                />
              </FormField>
              <FormField label="Password" error={errors.password?.message} required>
                <TextInput
                  id="password"
                  type="password"
                  autoComplete="current-password"
                  {...register('password')}
                />
              </FormField>
              <Button
                type="submit"
                variant="primary"
                w="full"
                loading={login.isPending}
                loadingText="Signing in…"
              >
                Sign In
              </Button>
              {login.isError && (
                <Text fontSize="sm" color="red.600" textAlign="center">
                  Login failed. Check credentials.
                </Text>
              )}
            </VStack>
          ) : (
            <VStack gap={4} align="stretch">
              {!otpMobile ? (
                <VStack
                  as="form"
                  gap={4}
                  align="stretch"
                  onSubmit={handleSubmit((d) => onSendOtp(d.username))}
                >
                  <FormField label="Mobile Number" required>
                    <TextInput
                      id="mobile-otp"
                      type="tel"
                      placeholder="9876543210"
                    />
                  </FormField>
                  <Button
                    type="submit"
                    variant="primary"
                    w="full"
                    loading={generateOtp.isPending}
                    loadingText="Sending…"
                  >
                    Send OTP
                  </Button>
                  {generateOtp.isError && (
                    <Text fontSize="sm" color="red.600" textAlign="center">
                      Could not send OTP. Try again.
                    </Text>
                  )}
                </VStack>
              ) : (
                <VStack
                  as="form"
                  gap={4}
                  align="stretch"
                  onSubmit={submitOtp(onVerifyOtpSubmit)}
                >
                  <Text fontSize="sm" color="gray.600">
                    OTP sent to <Box as="strong" color="black">{otpMobile}</Box>
                  </Text>
                  <FormField label="Enter OTP" error={otpErrors.otp?.message} required>
                    <TextInput
                      id="otp"
                      type="text"
                      inputMode="numeric"
                      maxLength={6}
                      placeholder="000000"
                      textAlign="center"
                      fontFamily="mono"
                      letterSpacing="0.5em"
                      {...regOtp('otp')}
                    />
                  </FormField>
                  <Button
                    type="submit"
                    variant="primary"
                    w="full"
                    disabled={verifyOtp.isPending || (otpCode?.length ?? 0) !== 6}
                    loading={verifyOtp.isPending}
                    loadingText="Verifying…"
                  >
                    Verify &amp; Sign In
                  </Button>
                  {verifyOtp.isError && (
                    <Text fontSize="sm" color="red.600" textAlign="center">
                      Invalid OTP. Try again.
                    </Text>
                  )}
                </VStack>
              )}
            </VStack>
          )}
        </Card.Body>
      </Card.Root>
    </Flex>
  );
}
