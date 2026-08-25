export const ChatHeader = ()=>{
     const menuItems = [
    { label: "새 상담 시작", active: true },
    { label: "최근 채팅" },
    { label: "상담 히스토리" },
    { label: "즐겨찾기" },
  ];

  return (
    <header className="flex h-full w-64 flex-col justify-between border-r border-slate-200 bg-white px-4 py-5">
      <div>
        <div className="mb-8 px-2">
          <span className="text-lg font-semibold text-slate-900">LawChat</span>
        </div>

        <nav className="flex flex-col gap-1">
          {menuItems.map(({ label, active }) => (
            <div
              key={label}
              className={`rounded-lg px-3 py-2.5 text-sm ${
                active ? "bg-violet-50 font-medium text-violet-700" : "text-slate-600"
              }`}
            >
              {label}
            </div>
          ))}
        </nav>
      </div>

      <div>
        <div className="mb-3 rounded-lg px-3 py-2.5 text-sm text-slate-600">설정</div>

        <div className="flex items-center gap-2.5 rounded-lg border border-slate-200 px-3 py-2.5">
          <div className="flex h-8 w-8 items-center justify-center rounded-full bg-slate-200 text-xs font-medium text-slate-600">
            길동
          </div>
          <div className="leading-tight">
            <p className="text-sm font-medium text-slate-900">홍길동 님</p>
            <p className="text-xs text-slate-400">일반 사용자</p>
          </div>
        </div>
      </div>
    </header>
  );
}