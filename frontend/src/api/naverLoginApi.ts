const naverApiKey =import.meta.env.VITE_NAVER_REST_API_KEY;
const naverRedirectUri = import.meta.env.VITE_NAVER_REDIRECT_URL;

export const NAVER_AUTH_URL=`https://nid.naver.com/oauth2.0/authorize?response_type=code&client_id=${naverApiKey}&redirect_uri=${naverRedirectUri}`;
// 보안 상 &state=${STATE} 값을 추가해 백이랑 추가검증 할 필요가 있지만 일단 지움