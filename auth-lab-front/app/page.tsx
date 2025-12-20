"use client";

import { useState } from "react";
import axios from "axios";
import { QRCodeSVG } from "qrcode.react"; // QR 코드 컴포넌트

export default function Home() {
  // === 상태 관리 ===
  const [step, setStep] = useState<"LOGIN" | "2FA" | "DASHBOARD">("LOGIN");
  
  // 입력값
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [code, setCode] = useState(""); // 2FA 인증번호

  // 서버 응답 데이터
  const [preAuthToken, setPreAuthToken] = useState("");
  const [token, setToken] = useState(""); // accessToken
  const [message, setMessage] = useState("");

  // TOTP 설정용
  const [totpSecret, setTotpSecret] = useState("");
  const [qrUrl, setQrUrl] = useState("");

  // === API 요청 함수 ===

  // 1. 1차 로그인 (ID/PW)
  const handleLogin = async () => {
    try {
      const res = await axios.post("http://localhost:8080/api/auth/login", {
        username,
        password,
      });

      console.log("Login Response:", res.data);

      if (res.data.requiresTwoFactor) {
        // 2차 인증 필요 -> 2FA 화면으로 전환
        setPreAuthToken(res.data.preAuthToken);
        setMessage(res.data.message);
        setStep("2FA");
      } else {
        // 인증 완료 -> 대시보드로 이동
        setToken(res.data.accessToken);
        setStep("DASHBOARD");
      }
    } catch (e: any) {
      alert("로그인 실패: " + (e.response?.data?.message || e.message));
    }
  };

  // 2. 2차 인증 (Code)
  const handleVerify2FA = async () => {
    try {
      const res = await axios.post("http://localhost:8080/api/auth/login/2fa", {
        preAuthToken,
        code,
      });
      // 성공 -> 대시보드로 이동
      setToken(res.data.accessToken);
      setStep("DASHBOARD");
    } catch (e: any) {
      alert("인증 실패: " + (e.response?.data?.message || e.message));
    }
  };

  // 3. TOTP 설정 키 발급 (QR 코드용)
  const handleSetupTotp = async () => {
    try {
      // 텍스트로 Secret Key 받아옴
      const res = await axios.get("http://localhost:8080/api/auth/totp/setup");
      const secret = res.data; 
      
      setTotpSecret(secret);
      // 구글 OTP 앱이 인식하는 URL 포맷 생성
      // otpauth://totp/라벨?secret=키&issuer=발급자
      const url = `otpauth://totp/AuthLab:${username}?secret=${secret}&issuer=AuthLab`;
      setQrUrl(url);
    } catch (e: any) {
      alert("설정 실패: " + e.message);
    }
  };

  // 4. TOTP 활성화
  const handleActivateTotp = async () => {
    try {
      await axios.post(
        "http://localhost:8080/api/auth/totp/activate",
        { secretKey: totpSecret, code },
        { headers: { Authorization: `Bearer ${token}` } } // 헤더에 토큰 필수
      );
      alert("TOTP가 성공적으로 활성화되었습니다! 다음 로그인부터 적용됩니다.");
      setQrUrl(""); // QR 닫기
      setCode("");
    } catch (e: any) {
      alert("활성화 실패: " + (e.response?.data?.message || e.message));
    }
  };

  // === UI 렌더링 ===
  return (
    <div className="flex flex-col items-center justify-center min-h-screen bg-gray-100 p-4">
      <div className="bg-white p-8 rounded-lg shadow-md w-full max-w-md">
        <h1 className="text-2xl font-bold mb-6 text-center text-gray-800">
          🔐 Auth Lab
        </h1>

        {/* STEP 1: 로그인 화면 */}
        {step === "LOGIN" && (
          <form 
            className="flex flex-col gap-4"
            onSubmit={(e) => {
              e.preventDefault();
              handleLogin();
            }}
          >
            <input
              className="border p-2 rounded text-black"
              placeholder="Username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              autoFocus
            />
            <input
              className="border p-2 rounded text-black"
              type="password"
              placeholder="Password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
            <button
              onClick={handleLogin}
              className="bg-blue-500 text-white p-2 rounded hover:bg-blue-600"
            >
              로그인
            </button>
          </form>
        )}

        {/* STEP 2: 2차 인증 화면 */}
        {step === "2FA" && (
          <form 
            className="flex flex-col gap-4"
            onSubmit={(e) => {
              e.preventDefault();
              handleVerify2FA();
            }}
          >
            <div className="text-center bg-yellow-100 p-2 rounded text-sm text-yellow-800 mb-2">
              ⚠️ {message}
            </div>
            <input
              className="border p-2 rounded text-black text-center text-xl tracking-widest"
              placeholder="000000"
              maxLength={6}
              value={code}
              onChange={(e) => setCode(e.target.value)}
              autoFocus
            />
            <button
              onClick={handleVerify2FA}
              className="bg-green-500 text-white p-2 rounded hover:bg-green-600"
            >
              인증 확인
            </button>
          </form>
        )}

        {/* STEP 3: 대시보드 (로그인 성공) */}
        {step === "DASHBOARD" && (
          <div className="flex flex-col gap-4">
            <div className="bg-green-100 p-4 rounded text-green-800 text-center">
              🎉 <strong>{username}</strong>님, 환영합니다!
            </div>
            
            <hr className="my-2" />

            <h3 className="font-bold text-gray-700">보안 설정</h3>
            
            {!qrUrl ? (
              <button
                onClick={handleSetupTotp}
                className="bg-purple-500 text-white p-2 rounded hover:bg-purple-600"
              >
                Google OTP (TOTP) 등록하기
              </button>
            ) : (
              <div className="flex flex-col items-center gap-4 bg-gray-50 p-4 rounded border">
                <p className="text-sm text-gray-600">아래 QR코드를 스캔하세요</p>
                
                {/* QR 코드 생성 라이브러리 사용 */}
                <QRCodeSVG value={qrUrl} size={150} />
                
                <p className="text-xs text-gray-400 break-all">{totpSecret}</p>

                <input
                  className="border p-2 rounded text-black w-full text-center"
                  placeholder="앱에 뜬 숫자 6자리"
                  value={code}
                  onChange={(e) => setCode(e.target.value)}
                />
                <button
                  onClick={handleActivateTotp}
                  className="bg-purple-600 text-white p-2 rounded w-full"
                >
                  활성화 완료
                </button>
              </div>
            )}

            <button
              onClick={() => window.location.reload()}
              className="mt-4 text-gray-400 hover:text-gray-600 underline"
            >
              로그아웃
            </button>
          </div>
        )}
      </div>
    </div>
  );
}